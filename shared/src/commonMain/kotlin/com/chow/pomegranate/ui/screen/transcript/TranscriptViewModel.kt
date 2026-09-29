package com.chow.pomegranate.ui.screen.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.TranscriptRepository
import com.chow.pomegranate.repository.Transcripts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching

/** 成绩页 UI 状态。 */
sealed interface TranscriptUiState {
    data object Loading : TranscriptUiState
    data object NotLoggedIn : TranscriptUiState
    data class Success(
        val transcripts: Transcripts,
        /** Explicitly submitted local keywords; no extra academic-affairs requests. */
        val courseKeyword: String = "",
        val levelKeyword: String = "",
    ) : TranscriptUiState
    data class Error(val throwable: Throwable) : TranscriptUiState
}

/** 成绩页 ViewModel。 */
@KoinViewModel
class TranscriptViewModel(
    private val repository: TranscriptRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<TranscriptUiState>
        field = MutableStateFlow<TranscriptUiState>(TranscriptUiState.Loading)

    init {
        refresh()
    }

    /** 从教务系统刷新成绩。 */
    fun refresh() {
        viewModelScope.launch {
            uiState.value = TranscriptUiState.Loading

            // 未登录时不请求受保护接口，先等待自动登录
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = TranscriptUiState.NotLoggedIn
                return@launch
            }
            uiState.value = runSuspendCatching { repository.getTranscripts() }
                .fold(
                    onSuccess = TranscriptUiState::Success,
                    onFailure = TranscriptUiState::Error,
                )
        }
    }

    fun submitCourseKeyword(keyword: String) = updateKeywords { copy(courseKeyword = keyword) }

    fun submitLevelKeyword(keyword: String) = updateKeywords { copy(levelKeyword = keyword) }

    private fun updateKeywords(transform: TranscriptUiState.Success.() -> TranscriptUiState.Success) {
        val current = uiState.value as? TranscriptUiState.Success ?: return
        uiState.value = current.transform()
    }
}
