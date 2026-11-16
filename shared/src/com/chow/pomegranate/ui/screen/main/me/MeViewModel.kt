package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.repository.WallpaperRepository
import com.chow.pomegranate.repository.AccountNicknameRepository
import com.chow.pomegranate.repository.accountNicknameKey
import com.chow.pomegranate.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** 我的页 ViewModel。 */
@KoinViewModel
class MeViewModel(
    private val preferenceRepository: AppPreferenceRepository,
    private val wallpaperRepository: WallpaperRepository,
    val sessionManager: SessionManager,
    nicknames: AccountNicknameRepository,
) : ViewModel() {
    val nickname: StateFlow<String?> = combine(sessionManager.userId, sessionManager.accounts, nicknames.names) { id, accounts, names ->
        accounts.firstOrNull { it.userId == id }?.let { names[accountNicknameKey(it.userId, it.isDemo)] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 性别。 */
    val gender: StateFlow<Gender>
        field = MutableStateFlow(Gender.Default)

    /** 校区。 */
    val campus: StateFlow<CampusOption>
        field = MutableStateFlow(CampusOption.Default)

    /** 主题色，`null` 表示使用默认色。 */
    val themeColor: StateFlow<Color?>
        field = MutableStateFlow<Color?>(null)

    /** 上次确认保存的主题色，用于取消时还原。 */
    private var savedThemeColor: Color? = null

    /** 每日壁纸地址，获取失败时为空。 */
    val wallpaperUrl: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    /** 应用私有目录里的本地头像；不与学校账户资料同步。 */
    val avatarUri: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            gender.value = Gender.entries.getOrNull(readOrdinal(PreferenceKeys.GENDER) ?: -1)
                ?: Gender.Default
            campus.value = CampusOption.fromOrdinal(readOrdinal(PreferenceKeys.CAMPUS))

            // 主题色按 RGB 十六进制保存，按通道还原
            themeColor.value = preferenceRepository.get(PreferenceKeys.PRIMARY_COLOR)?.toColor()
            savedThemeColor = themeColor.value
            avatarUri.value = preferenceRepository.get(PreferenceKeys.LOCAL_AVATAR_URI)
        }

        viewModelScope.launch {
            preferenceRepository.observe(PreferenceKeys.PRIMARY_COLOR).collect { themeColor.value = it?.toColor() }
        }
        viewModelScope.launch {
            preferenceRepository.observe(PreferenceKeys.LOCAL_AVATAR_URI).collect { avatarUri.value = it }
        }

        viewModelScope.launch {
            wallpaperUrl.value = wallpaperRepository.getTodayWallpaper()?.urlString
        }
    }

    fun saveThemeColor(color: Color?, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val result = com.chow.pomegranate.utils.runSuspendCatching {
                preferenceRepository.set(PreferenceKeys.PRIMARY_COLOR, color?.toHexString())
                themeColor.value = color
                savedThemeColor = color
            }
            onResult(result.exceptionOrNull())
        }
    }

    fun saveAvatar(uri: String?, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val result = com.chow.pomegranate.utils.runSuspendCatching {
                val previous = preferenceRepository.get(PreferenceKeys.LOCAL_AVATAR_URI)
                preferenceRepository.set(PreferenceKeys.LOCAL_AVATAR_URI, uri)
                avatarUri.value = uri
                if (previous != null && previous != uri) deleteLocalAvatar(previous)
            }
            onResult(result.exceptionOrNull())
        }
    }

    /** 切换性别并保存。 */
    fun toggleGender() {
        val next = gender.value.toggle()
        gender.value = next
        save(PreferenceKeys.GENDER, next.ordinal.toString())
    }

    /** 切换校区并保存。 */
    fun toggleCampus() {
        val next = campus.value.toggle()
        campus.value = next
        save(PreferenceKeys.CAMPUS, next.ordinal.toString())
    }

    /** 设置主题色。 */
    fun setThemeColor(color: Color) {
        themeColor.value = color

        // 主题直接观察该偏好，拖动时实时生效
        save(PreferenceKeys.PRIMARY_COLOR, color.toHexString())
    }

    /**
     * 保存当前主题色。
     *
     * 预览时已经写入偏好，这里只确认基线，供之后取消时还原。
     */
    fun commitThemeColor() {
        savedThemeColor = themeColor.value
    }

    /**
     * 取消本次配色，还原为上次保存的颜色。
     *
     * 色盘是实时生效的，取消不能只是关闭弹窗，必须把颜色改回去。
     */
    fun cancelThemeColor() {
        restoreThemeColor(savedThemeColor)
    }

    /**
     * 重置主题色为默认色。
     *
     * 主题直接观察该偏好，清掉偏好即回到默认色。
     */
    fun resetThemeColor() {
        restoreThemeColor(null)
    }

    fun setLocalAvatar(uri: String) {
        val previousUri = avatarUri.value
        avatarUri.value = uri
        viewModelScope.launch {
            preferenceRepository.set(PreferenceKeys.LOCAL_AVATAR_URI, uri)

            // 每次换头像都会生成新文件，旧副本不再被引用，直接删掉避免占用空间
            if (previousUri != null && previousUri != uri) {
                deleteLocalAvatar(previousUri)
            }
        }
    }

    fun clearLocalAvatar() {
        val previousUri = avatarUri.value
        avatarUri.value = null
        viewModelScope.launch {
            if (previousUri != null) {
                deleteLocalAvatar(previousUri)
            }
            preferenceRepository.remove(PreferenceKeys.LOCAL_AVATAR_URI)
        }
    }

    /** 把主题色还原为 [color]，`null` 表示默认色。 */
    private fun restoreThemeColor(color: Color?) {
        themeColor.value = color

        viewModelScope.launch {
            if (color == null) {
                preferenceRepository.remove(PreferenceKeys.PRIMARY_COLOR)
            } else {
                preferenceRepository.set(PreferenceKeys.PRIMARY_COLOR, color.toHexString())
            }
        }
    }

    /** 保存偏好。 */
    private fun save(
        key: String,
        value: String,
    ) {
        viewModelScope.launch {
            preferenceRepository.set(key, value)
        }
    }

    /** 读取偏好下标。 */
    private suspend fun readOrdinal(key: String): Int? =
        preferenceRepository.get(key)?.toIntOrNull()
}

/**
 * 转换为 ARGB 十六进制字符串，用于持久化（保留透明度）。
 */
internal fun Color.toHexString(): String {
    val alpha = (this.alpha * 255).toInt() and 0xFF
    val red = (this.red * 255).toInt() and 0xFF
    val green = (this.green * 255).toInt() and 0xFF
    val blue = (this.blue * 255).toInt() and 0xFF

    val value = (alpha.toLong() shl 24) or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()

    return value.toString(16).padStart(8, '0')
}

/**
 * 把 ARGB 十六进制字符串还原为颜色。
 *
 * 直接使用 `Color(ULong)` 需要打包后的 64 位值，这里按通道构造，避免颜色空间错乱。
 */
internal fun String.toColor(): Color? {
    val hex = removePrefix("#")
    val value = hex.toLongOrNull(16) ?: return null
    if (value !in 0x00000000..0xFFFFFFFF) return null

    // 6 位按不透明处理，8 位含透明度
    val alpha = if (hex.length < 8) 0xFF else ((value shr 24) and 0xFF).toInt()

    return Color(
        red = ((value shr 16) and 0xFF).toInt(),
        green = ((value shr 8) and 0xFF).toInt(),
        blue = (value and 0xFF).toInt(),
        alpha = alpha,
    )
}
