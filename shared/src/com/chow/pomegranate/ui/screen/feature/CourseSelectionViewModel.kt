package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.CourseSelectionOverview
import com.chow.pomegranate.repository.CourseSelectionRepository
import com.chow.pomegranate.service.academic.affairs.model.SelectableCourse
import com.chow.pomegranate.service.academic.affairs.model.CourseOperateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait

/** 选课页 UI 状态。 */
sealed interface CourseSelectionUiState {
    data object Loading : CourseSelectionUiState

    /** 尚未登录，不能请求选课接口。 */
    data object NotLoggedIn : CourseSelectionUiState
    data class Error(val throwable: Throwable) : CourseSelectionUiState
    data class Success(
        val overview: CourseSelectionOverview,
        val query: String = "",
        val searchResults: List<SelectableCourse> = emptyList(),
        val isSearching: Boolean = false,
        val isOperating: Boolean = false,
        /** 待提示的事件；文案由界面决定，ViewModel 不持有中文。 */
        val notice: CourseSelectionNotice? = null,
    ) : CourseSelectionUiState
}

/**
 * 需要提示给用户的事件。
 *
 * ViewModel 不是 `@Composable`，拿不到 `stringResource`；这里只表达"发生了什么"，
 * 由界面映射成资源文案。
 */
enum class CourseSelectionNotice {
    /** 搜索失败。 */
    SearchFailed,
}

/** 选课页 ViewModel。 */
@KoinViewModel
class CourseSelectionViewModel(
    private val repository: CourseSelectionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    val uiState: StateFlow<CourseSelectionUiState>
        field = MutableStateFlow<CourseSelectionUiState>(CourseSelectionUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.value = CourseSelectionUiState.Loading

            // 未登录时不请求受保护接口
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = CourseSelectionUiState.NotLoggedIn
                return@launch
            }

            uiState.value = runCatching { repository.getOverview() }
                .fold(CourseSelectionUiState::Success, CourseSelectionUiState::Error)
        }
    }

    fun search(query: String) {
        val state = uiState.value as? CourseSelectionUiState.Success ?: return
        viewModelScope.launch {
            uiState.value = state.copy(query = query, isSearching = true, notice = null)
            uiState.value = runCatching { repository.search(query) }.fold(
                onSuccess = { state.copy(query = query, searchResults = it, isSearching = false) },
                onFailure = {
                    state.copy(
                        query = query,
                        isSearching = false,
                        notice = CourseSelectionNotice.SearchFailed,
                    )
                },
            )
        }
    }

    /** 提示已展示，清除事件。 */
    fun consumeNotice() {
        val state = uiState.value as? CourseSelectionUiState.Success ?: return
        uiState.value = state.copy(notice = null)
    }
}

/** 选课操作确认弹窗 ViewModel。 */
@KoinViewModel
class CourseOperationViewModel(
    private val repository: CourseSelectionRepository,
) : ViewModel() {
    fun operate(route: CourseOperationRoute, onResult: (Result<CourseOperateResult>) -> Unit) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    when (route.operation) {
                        CourseOperation.Select -> repository.select(route.courseId)
                        CourseOperation.Drop -> repository.drop(route.courseId)
                    }
                },
            )
        }
    }
}
