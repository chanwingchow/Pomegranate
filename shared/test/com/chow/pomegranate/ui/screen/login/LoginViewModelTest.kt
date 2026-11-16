package com.chow.pomegranate.ui.screen.login

import com.chow.pomegranate.session.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun connectionChangesRespectSecondClassroomOnlyAndRefreshAcademicCaptcha() = runTest {
        val session = FakeLoginSession()
        val model = LoginViewModel(session)
        model.connectionChanged(LoginTarget.SecondClassroom)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, session.captchaRequests)
        model.connectionChanged(LoginTarget.Both)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, session.captchaRequests)
        model.connectionChanged()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, session.captchaRequests)
    }

    @Test fun secondClassroomRequiresNoAcademicPasswordOrCaptchaAndForwardsSaveChoice() = runTest {
        val session = FakeLoginSession()
        val model = LoginViewModel(session)
        model.selectTarget(LoginTarget.SecondClassroom)
        var completed = false
        model.login("20240000001", "", "second", "", saveAccount = false) { completed = true }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(completed)
        assertEquals(0, session.captchaRequests)
        assertEquals(LoginTarget.SecondClassroom, session.request!!.target)
        assertFalse(session.request!!.saveAccount)
        assertFalse(model.isLoggingIn.value)
    }

    @Test fun savedAccountCanSubmitWithoutReenteringAcademicPassword() = runTest {
        val session = FakeLoginSession().apply { accounts.value = listOf(Account("20240000001", true)) }
        val model = LoginViewModel(session)
        model.selectTarget(LoginTarget.Academic)
        dispatcher.scheduler.advanceUntilIdle()
        model.login("20240000001", "", "", "abcd") {}
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(LoginTarget.Academic, session.request!!.target)
        assertEquals("", session.request!!.password)
    }

    @Test fun secondClassroomFailureDoesNotStartAcademicCaptchaRequests() = runTest {
        val session = FakeLoginSession().apply { fail = true }
        val model = LoginViewModel(session)
        model.selectTarget(LoginTarget.SecondClassroom)
        model.login("20240000001", "", "second", "") { error("Should not succeed") }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, session.captchaRequests)
        assertEquals("登录失败", model.errors.first().detail)
        assertFalse(model.isLoggingIn.value)
    }
    @Test fun repeatedValidationFailuresAreDeliveredAsSeparateSnackbarEvents() = runTest {
        val model = LoginViewModel(FakeLoginSession())
        repeat(2) {
            model.login("", "", "", "") { error("Should not succeed") }
            assertNull(model.errors.first().detail)
        }
    }
}

private class FakeLoginSession : SessionManager {
    override val userId = MutableStateFlow<String?>(null)
    override val state = MutableStateFlow<SessionState>(SessionState.Inactive)
    override val accounts = MutableStateFlow(emptyList<Account>())
    var request: LoginRequest? = null
    var captchaRequests = 0
    var fail = false
    override suspend fun getCaptcha(): ByteArray { captchaRequests++; return byteArrayOf() }
    override suspend fun signIn(request: LoginRequest) {
        this.request = request
        if (fail) error("登录失败")
    }
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) = error("Legacy login should not be used")
    override suspend fun autoLogin() = Unit
    override suspend fun switchAccount(userId: String) = Unit
    override suspend fun deleteAccount(userId: String) = Unit
    override suspend fun refresh() = Unit
    override suspend fun logout() = Unit
    override suspend fun awaitLogin(): Boolean? = null
}
