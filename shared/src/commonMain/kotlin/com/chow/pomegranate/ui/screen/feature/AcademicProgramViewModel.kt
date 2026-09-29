package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.AcademicProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching

/** 培养方案页 UI 状态。 */
sealed interface AcademicProgramUiState {
    /** 加载中。 */
    data object Loading : AcademicProgramUiState

    /** 尚未登录，不能请求受保护接口。 */
    data object NotLoggedIn : AcademicProgramUiState

    /**
     * 加载成功。
     *
     * @property html 培养方案 HTML，直接交给 HTML 渲染组件展示
     */
    data class Success(val html: String) : AcademicProgramUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : AcademicProgramUiState
}

/** 培养方案页 ViewModel。 */
@KoinViewModel
class AcademicProgramViewModel(
    private val repository: AcademicProgramRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<AcademicProgramUiState>
        field = MutableStateFlow<AcademicProgramUiState>(AcademicProgramUiState.Loading)

    init {
        refresh()
    }

    /** 刷新培养方案。 */
    fun refresh() {
        viewModelScope.launch {
            uiState.value = AcademicProgramUiState.Loading

            // 未登录时不请求受保护接口，先等待自动登录
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = AcademicProgramUiState.NotLoggedIn
                return@launch
            }
            uiState.value = runSuspendCatching { repository.getAcademicProgram().html }
                .fold(
                    onSuccess = AcademicProgramUiState::Success,
                    onFailure = AcademicProgramUiState::Error,
                )
        }
    }
}
