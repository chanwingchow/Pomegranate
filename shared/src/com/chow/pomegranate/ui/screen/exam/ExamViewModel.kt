package com.chow.pomegranate.ui.screen.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.ExamRepository
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching

/** 考试安排页 UI 状态。 */
sealed interface ExamUiState {
    /** 加载中。 */
    data object Loading : ExamUiState

    /** 尚未登录，不能请求受保护接口。 */
    data object NotLoggedIn : ExamUiState

    /** 加载成功。 */
    data class Success(val examSchedule: ExamSchedule) : ExamUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : ExamUiState
}

/** 考试安排页 ViewModel。 */
@KoinViewModel
class ExamViewModel(
    private val repository: ExamRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<ExamUiState>
        field = MutableStateFlow<ExamUiState>(ExamUiState.Loading)

    init {
        refresh()
    }

    /** 从教务系统刷新考试安排。 */
    fun refresh() {
        viewModelScope.launch {
            uiState.value = ExamUiState.Loading

            // 未登录时不请求受保护接口，先等待自动登录
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = ExamUiState.NotLoggedIn
                return@launch
            }
            uiState.value = runSuspendCatching { repository.getExamSchedule() }
                .fold(
                    onSuccess = ExamUiState::Success,
                    onFailure = ExamUiState::Error,
                )
        }
    }
}
