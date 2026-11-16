package com.chow.pomegranate.ui.screen.main.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.repository.TimetableRepository
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** 课表页 UI 状态。 */
sealed interface TimetableUiState {
    /** 加载中。 */
    data object Loading : TimetableUiState

    /** 尚未登录，不能请求课表。 */
    data object NotLoggedIn : TimetableUiState

    /** 加载成功。 */
    data class Success(
        val timetable: Timetable,
        val display: TimetableDisplay,
    ) : TimetableUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : TimetableUiState
}

/** 课表页 ViewModel。 */
@KoinViewModel
class TimetableViewModel(
    private val repository: TimetableRepository,
    private val preferenceRepository: AppPreferenceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<TimetableUiState>
        field = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)

    /** 周次，第 0 周表示开学前一周。 */
    val week: MutableStateFlow<Int>
        field = MutableStateFlow(0)

    /** 已加载的课表：切换周次时复用它重新计算，不重新请求。 */
    private var loadedTimetable: Timetable? = null

    init {
        viewModelScope.launch {
            week.value = preferenceRepository.get(PreferenceKeys.WEEK)?.toIntOrNull() ?: 0
        }

        refresh()
    }

    /** 保存周次，并按该周重新计算课表。 */
    fun setWeek(value: Int) {
        week.value = value

        // 已有数据时立即重算，避免重新请求
        loadedTimetable?.let { uiState.value = successState(it) }

        viewModelScope.launch {
            preferenceRepository.set(PreferenceKeys.WEEK, value.toString())
        }
    }

    /** 从教务系统刷新课表。 */
    fun refresh() {
        viewModelScope.launch {
            uiState.value = TimetableUiState.Loading

            // 未登录时不请求受保护接口，先等待自动登录
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = TimetableUiState.NotLoggedIn
                return@launch
            }

            uiState.value = runSuspendCatching { repository.getTimetable() }
                .fold(
                    onSuccess = { timetable ->
                        loadedTimetable = timetable
                        successState(timetable)
                    },
                    onFailure = TimetableUiState::Error,
                )
        }
    }

    /** 按当前周次生成成功状态。 */
    private fun successState(timetable: Timetable) = TimetableUiState.Success(
        timetable = timetable,
        display = TimetableDisplay.of(timetable.courses, week = week.value),
    )
}
