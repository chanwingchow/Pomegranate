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
    ) : TranscriptUiState
    data class Error(val throwable: Throwable) : TranscriptUiState
}

data class TranscriptFilters(
    val courseKeyword: String = "",
    val levelKeyword: String = "",
    val semester: String? = null,
    val failedOnly: Boolean = false,
)

/** 成绩页 ViewModel。 */
@KoinViewModel
class TranscriptViewModel(
    private val repository: TranscriptRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<TranscriptUiState>
        field = MutableStateFlow<TranscriptUiState>(TranscriptUiState.Loading)

    val isRefreshing: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val filters: StateFlow<TranscriptFilters>
        field = MutableStateFlow(TranscriptFilters())
    private var filtersOwner: String? = null

    init {
        refresh()
    }

    /** 从教务系统刷新成绩。 */
    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            if (uiState.value !is TranscriptUiState.Success) uiState.value = TranscriptUiState.Loading
            try {
                if (!sessionManager.isLoggedInWithWait()) {
                    uiState.value = TranscriptUiState.NotLoggedIn
                    return@launch
                }
                uiState.value = runSuspendCatching { repository.getTranscripts() }.fold(
                    onSuccess = { transcripts ->
                        if (filtersOwner != transcripts.course.userId) {
                            filters.value = TranscriptFilters(semester = transcripts.course.items.defaultTranscriptSemester())
                            filtersOwner = transcripts.course.userId
                        }
                        TranscriptUiState.Success(transcripts)
                    },
                    onFailure = TranscriptUiState::Error,
                )
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun submitCourseKeyword(keyword: String) { filters.value = filters.value.copy(courseKeyword = keyword) }
    fun submitLevelKeyword(keyword: String) { filters.value = filters.value.copy(levelKeyword = keyword) }
    fun selectSemester(semester: String?) { filters.value = filters.value.copy(semester = semester) }
    fun showFailedOnly(value: Boolean) { filters.value = filters.value.copy(failedOnly = value) }
}
