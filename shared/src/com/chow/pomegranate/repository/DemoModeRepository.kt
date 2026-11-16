package com.chow.pomegranate.repository

import com.chow.pomegranate.data.local.PreferenceKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

/** 演示模式统一入口；读取完成前不允许启动真实会话。 */
@Single
class DemoModeRepository(private val preferences: AppPreferenceRepository) {
    private val mutex = Mutex()
    private val mutableEnabled = MutableStateFlow<Boolean?>(null)
    val enabled: StateFlow<Boolean?> = mutableEnabled

    suspend fun isEnabled(): Boolean = mutex.withLock {
        mutableEnabled.value ?: (preferences.get(PreferenceKeys.DEMO_MODE) == "true")
            .also { mutableEnabled.value = it }
    }

    suspend fun setEnabled(enabled: Boolean) = mutex.withLock {
        preferences.set(PreferenceKeys.DEMO_MODE, enabled.toString())
        mutableEnabled.value = enabled
    }
}
