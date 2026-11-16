package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.session.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StudyCatalogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }
    private fun model(): StudyCatalogViewModel {
        val session = LoggedInCatalogSession()
        return StudyCatalogViewModel(StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(100)),
            session, testSchoolTimetables(session))
    }

    @Test fun returningReusesLoadedCatalogAndKeepsItsFilters() = runTest {
        val model = model()
        model.ensureLoaded(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceUntilIdle()
        model.selectTab(1)
        dispatcher.scheduler.advanceUntilIdle()
        val loaded = model.state.value
        model.ensureLoaded(AcademicFeature.Extracurricular)
        assertSame(loaded, model.state.value)
        assertEquals(1, model.state.value.tab)
        model.refresh()
        assertTrue(model.state.value.refreshing)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.refreshing)
    }

    @Test fun returningDuringLoadKeepsTheOriginalRequest() = runTest {
        val model = model()
        model.ensureLoaded(AcademicFeature.Extracurricular)
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(50)
        model.ensureLoaded(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceTimeBy(51)
        dispatcher.scheduler.runCurrent()
        assertFalse(model.state.value.loading)
        assertNotNull(model.state.value.catalog)
    }

    @Test fun switchingAccountDropsCachedTabsAndFetchesAgain() = runTest {
        val session = LoggedInCatalogSession()
        val model = StudyCatalogViewModel(StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(100)), session, testSchoolTimetables(session))
        model.ensureLoaded(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceUntilIdle()
        model.selectTab(1)
        dispatcher.scheduler.advanceUntilIdle()
        session.userId.value = "another-user"
        model.ensureLoaded(AcademicFeature.Extracurricular)
        assertTrue(model.state.value.loading)
        assertFalse(model.tabStates.value.containsKey(0))
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(model.state.value.catalog)
    }

    @Test fun loginAfterNotLoggedInRetriesTheRequest() = runTest {
        val session = LoggedInCatalogSession()
        session.state.value = SessionState.Inactive
        session.loginAvailable = null
        val model = StudyCatalogViewModel(StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(100)), session, testSchoolTimetables(session))
        model.ensureLoaded(AcademicFeature.Teachers)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.state.value.notLoggedIn)
        session.state.value = SessionState.Active
        session.loginAvailable = false
        model.ensureLoaded(AcademicFeature.Teachers)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.notLoggedIn)
        assertNotNull(model.state.value.catalog)
    }

    @Test fun tabDataStaysSeparateAndLoadedPagesAreReused() = runTest {
        val model = model()
        model.load(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceUntilIdle()
        val grades = model.state.value.catalog
        model.selectTab(1)
        dispatcher.scheduler.advanceUntilIdle()
        val events = model.state.value.catalog
        assertEquals("活动", events!!.groups.single().title)
        assertEquals("成绩", model.tabStates.value[0]!!.catalog!!.groups.single().title)
        model.selectTab(0)
        assertSame(grades, model.state.value.catalog)
        assertFalse(model.state.value.loading)
        model.selectTab(1)
        assertSame(events, model.state.value.catalog)
    }

    @Test fun interruptedRequestCannotReplaceThePageTheUserSwipedTo() = runTest {
        val model = model()
        model.load(AcademicFeature.Extracurricular)
        dispatcher.scheduler.runCurrent()
        model.selectTab(1)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, model.state.value.tab)
        assertEquals("活动", model.state.value.catalog!!.groups.single().title)
        model.selectTab(0)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("成绩", model.state.value.catalog!!.groups.single().title)
        assertFalse(model.state.value.loading)
    }

    @Test fun rapidlyReturningBeforeARequestStartsKeepsTheRequestedPage() = runTest {
        val model = model()
        model.load(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceUntilIdle()
        model.selectTab(1)
        model.selectTab(0)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, model.state.value.tab)
        assertEquals("成绩", model.state.value.catalog!!.groups.single().title)
    }

    @Test fun leavingDuringRefreshDoesNotLeaveACachedPageRefreshingForever() = runTest {
        val model = model()
        model.load(AcademicFeature.Extracurricular)
        dispatcher.scheduler.advanceUntilIdle()
        model.refresh()
        dispatcher.scheduler.runCurrent()
        assertTrue(model.state.value.refreshing)
        model.selectTab(2)
        dispatcher.scheduler.advanceUntilIdle()
        model.selectTab(0)
        assertFalse(model.state.value.refreshing)
        assertNotNull(model.state.value.catalog)
    }
}

private class LoggedInCatalogSession : SessionManager {
    override val userId = MutableStateFlow<String?>("demo")
    override val state = MutableStateFlow<SessionState>(SessionState.Active)
    override val accounts = MutableStateFlow(emptyList<Account>())
    var loginAvailable: Boolean? = false
    override suspend fun awaitLogin(): Boolean? = loginAvailable
    override suspend fun getCaptcha(): ByteArray = error("Unused")
    override suspend fun signIn(request: LoginRequest) = Unit
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) = Unit
    override suspend fun autoLogin() = Unit
    override suspend fun switchAccount(userId: String) = Unit
    override suspend fun deleteAccount(userId: String) = Unit
    override suspend fun refresh() = Unit
    override suspend fun logout() = Unit
}
