package com.chow.pomegranate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.service.update.AppRelease
import com.chow.pomegranate.service.update.AppUpdate
import com.chow.pomegranate.service.update.AppUpdates
import com.chow.pomegranate.service.update.UpdateFailure
import com.chow.pomegranate.service.update.UpdateInstaller
import com.chow.pomegranate.service.update.UpdatePackage
import com.chow.pomegranate.service.update.UpdateTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal sealed interface AppUpdateState {
    data class Available(val update: AppUpdate, val error: String? = null) : AppUpdateState
    data class Downloading(val update: AppUpdate, val progress: Float = 0f) : AppUpdateState
    data class Ready(
        val update: AppUpdate,
        val asset: UpdatePackage,
        val path: Path,
        val opening: Boolean = false,
        val message: String? = null,
    ) : AppUpdateState
}

internal sealed interface ReleaseHistoryState {
    data object Loading : ReleaseHistoryState
    data class Loaded(val releases: List<AppRelease>) : ReleaseHistoryState
    data class Failed(val message: String) : ReleaseHistoryState
}

@KoinViewModel
internal class AppUpdateViewModel(private val preferences: AppPreferenceRepository) : ViewModel() {
    private val updates = AppUpdates()
    private val mutableState = MutableStateFlow<AppUpdateState?>(null)
    val state = mutableState.asStateFlow()
    private val mutableLatest = MutableStateFlow<AppUpdate?>(null)
    val latest = mutableLatest.asStateFlow()
    private val mutableLatestResolved = MutableStateFlow(false)
    val latestResolved = mutableLatestResolved.asStateFlow()
    private val mutableStartupPromptPending = MutableStateFlow(false)
    val startupPromptPending = mutableStartupPromptPending.asStateFlow()
    val startupPromptsEnabled = preferences.observe(PreferenceKeys.STARTUP_UPDATE_PROMPTS)
        .map { it != "false" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val mutableStartupPromptsError = MutableStateFlow<String?>(null)
    val startupPromptsError = mutableStartupPromptsError.asStateFlow()
    private val mutableHistory = MutableStateFlow<ReleaseHistoryState>(ReleaseHistoryState.Loading)
    val history = mutableHistory.asStateFlow()
    private var latestRequest: Job? = null
    private var historyRequest: Job? = null
    private var operation: Job? = null

    fun checkAtStartup(target: UpdateTarget?) {
        if (latestRequest != null) return
        latestRequest = viewModelScope.launch {
            try {
                val update = updates.latestUpdate(BuildConfig.versionName, target)
                mutableLatest.value = update
                if (update != null) {
                    mutableStartupPromptPending.value = shouldShowStartupUpdate(
                        version = update.release.version,
                        enabled = preferences.get(PreferenceKeys.STARTUP_UPDATE_PROMPTS) != "false",
                        skipped = preferences.get(PreferenceKeys.SKIPPED_UPDATE_VERSIONS),
                        deferred = preferences.get(PreferenceKeys.DEFERRED_UPDATE),
                        nowMillis = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A failed background check must not interrupt startup.
            } finally {
                mutableLatestResolved.value = true
            }
        }
    }

    fun takeStartupPrompt(): Boolean {
        val pending = mutableStartupPromptPending.value
        mutableStartupPromptPending.value = false
        return pending && startupPromptsEnabled.value
    }

    fun setStartupPromptsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            mutableStartupPromptsError.value = null
            try {
                preferences.set(PreferenceKeys.STARTUP_UPDATE_PROMPTS, enabled.toString())
                if (!enabled) mutableStartupPromptPending.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableStartupPromptsError.value = "无法保存更新提示设置，请重试。"
            }
        }
    }

    fun postponeStartupPrompt(skipVersion: Boolean, onSaved: () -> Unit) {
        val available = mutableState.value as? AppUpdateState.Available ?: return
        if (operation?.isActive == true) return
        val version = available.update.release.version
        operation = viewModelScope.launch {
            try {
                if (skipVersion) {
                    val skipped = preferences.get(PreferenceKeys.SKIPPED_UPDATE_VERSIONS)
                        ?.split(',').orEmpty()
                    preferences.set(
                        PreferenceKeys.SKIPPED_UPDATE_VERSIONS,
                        (skipped + version).filter(String::isNotEmpty).distinct().joinToString(","),
                    )
                } else {
                    val until = Clock.System.now() + 1.days
                    preferences.set(PreferenceKeys.DEFERRED_UPDATE, "$version:${until.toEpochMilliseconds()}")
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = available.copy(error = "无法保存更新提醒选项，请重试。")
            }
        }
    }

    fun loadHistory() {
        if (historyRequest?.isActive == true || mutableHistory.value is ReleaseHistoryState.Loaded) return
        mutableHistory.value = ReleaseHistoryState.Loading
        historyRequest = viewModelScope.launch {
            mutableHistory.value = try {
                ReleaseHistoryState.Loaded(updates.releases())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReleaseHistoryState.Failed(error.updateMessage("版本列表加载失败，请检查网络后重试。"))
            }
        }
    }

    fun openUpdate() {
        val update = mutableLatest.value ?: return
        if (mutableState.value == null) mutableState.value = AppUpdateState.Available(update)
    }

    fun dismissUpdate(): Boolean {
        if ((mutableState.value as? AppUpdateState.Ready)?.opening == true) return false
        cancelDownload()
        mutableState.value = null
        return true
    }

    fun download(asset: UpdatePackage, installer: UpdateInstaller) {
        val current = mutableState.value as? AppUpdateState.Available ?: return
        if (operation?.isActive == true || asset !in current.update.packages) return
        val update = current.update
        val downloading = AppUpdateState.Downloading(update)
        mutableState.value = downloading
        operation = viewModelScope.launch {
            mutableState.value = try {
                val path = updates.download(asset, installer.directory) { progress ->
                    ensureActive()
                    mutableState.update { state ->
                        if (state is AppUpdateState.Downloading && state.update === update) {
                            state.copy(progress = progress)
                        } else {
                            state
                        }
                    }
                }
                AppUpdateState.Ready(update, asset, path)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ensureActive()
                AppUpdateState.Available(update, error.updateMessage("下载未完成，请检查网络和剩余空间后重试。"))
            }
        }
    }

    fun cancelDownload() {
        val current = mutableState.value as? AppUpdateState.Downloading ?: return
        operation?.cancel()
        mutableState.value = AppUpdateState.Available(current.update)
    }

    fun openInstaller(installer: UpdateInstaller) {
        val current = mutableState.value as? AppUpdateState.Ready ?: return
        if (operation?.isActive == true) return
        mutableState.value = current.copy(opening = true, message = null)
        operation = viewModelScope.launch {
            try {
                updates.verify(current.path, current.asset)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = AppUpdateState.Available(
                    current.update, error.updateMessage("无法读取安装包，请重新下载。"),
                )
                return@launch
            }
            val message = try {
                installer.open(current.path)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                error.updateMessage("无法打开系统安装程序，请前往发布页。")
            }
            mutableState.value = current.copy(message = message)
        }
    }

    override fun onCleared() {
        updates.close()
    }

    private fun Exception.updateMessage(fallback: String): String =
        (this as? UpdateFailure)?.message ?: fallback
}

internal fun shouldShowStartupUpdate(
    version: String,
    enabled: Boolean,
    skipped: String?,
    deferred: String?,
    nowMillis: Long,
): Boolean {
    if (!enabled || version in skipped?.split(',').orEmpty()) return false
    val deferredVersion = deferred?.substringBefore(':')
    val until = deferred?.substringAfter(':', "")?.toLongOrNull()
    return deferredVersion != version || until == null || nowMillis >= until
}
