package com.chow.pomegranate.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** 设置页 ViewModel。 */
@KoinViewModel
class SettingsViewModel(
    private val repository: AppPreferenceRepository,
) : ViewModel() {
    /** 主题模式。 */
    val themeMode: MutableStateFlow<ThemeMode>
        field = MutableStateFlow(ThemeMode.Auto)

    /** 周次。 */
    val week: MutableStateFlow<Int>
        field = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            themeMode.value = readThemeMode()
            week.value = repository.get(PreferenceKeys.WEEK)?.toIntOrNull() ?: 0
        }
    }

    /** 保存主题模式。 */
    fun setThemeMode(value: ThemeMode) {
        themeMode.value = value

        viewModelScope.launch {
            repository.set(PreferenceKeys.THEME_MODE, value.ordinal.toString())
        }
    }

    /** 保存周次。 */
    fun setWeek(value: Int) {
        week.value = value

        viewModelScope.launch {
            repository.set(PreferenceKeys.WEEK, value.toString())
        }
    }

    /** 读取已保存的主题模式。 */
    private suspend fun readThemeMode(): ThemeMode {
        val ordinal = repository.get(PreferenceKeys.THEME_MODE)?.toIntOrNull()

        return ThemeMode.entries.getOrNull(ordinal ?: -1) ?: ThemeMode.Auto
    }
}
