package com.chow.pomegranate.ui.screen.transcript

import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.TranscriptRepository
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptStateTest {
    @Test fun refreshKeepsExplicitAllSemestersQueriesAndFailedFilter() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val model = TranscriptViewModel(TranscriptRepository(MockAcademicAffairs(0, 0)), TranscriptSession())
        try {
            advanceUntilIdle()
            assertTrue(model.uiState.value is TranscriptUiState.Success)
            assertEquals(Semester.now().toString(), model.filters.value.semester)
            model.selectSemester(null)
            model.submitCourseKeyword("程序")
            model.submitLevelKeyword("英语")
            model.showFailedOnly(true)
            val chosen = model.filters.value
            model.refresh()
            advanceUntilIdle()
            assertEquals(chosen, model.filters.value)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}

private class TranscriptSession : SessionManager {
    override val userId = MutableStateFlow<String?>("demo")
    override val state = MutableStateFlow<SessionState>(SessionState.Active)
    override val accounts = MutableStateFlow(emptyList<Account>())
    override suspend fun awaitLogin(): Boolean = false
    override suspend fun getCaptcha(): ByteArray = error("Unused")
    override suspend fun signIn(request: LoginRequest) = Unit
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) = Unit
    override suspend fun autoLogin() = Unit
    override suspend fun switchAccount(userId: String) = Unit
    override suspend fun deleteAccount(userId: String) = Unit
    override suspend fun refresh() = Unit
    override suspend fun logout() = Unit
}
