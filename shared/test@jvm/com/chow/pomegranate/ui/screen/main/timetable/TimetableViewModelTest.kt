package com.chow.pomegranate.ui.screen.main.timetable

import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test fun aNewSessionStartsOnTodayInsteadOfThePreviouslyBrowsedWeek() = runTest(dispatcher) {
        withFixture { preferences, service, model ->
            preferences.set(PreferenceKeys.WEEK, "8")
            val owner = service.enrollment.getTimetable(Semester.now()).ownerKey()
            preferences.set("timetable_last_week:$owner", "9")
            val first = model()
            advanceUntilIdle()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            assertEquals(8, first.week.value)
            assertEquals(today, first.focusedDate.value)
            first.setWeek(9)
            first.selectDate(today.plus(DatePeriod(days = 7)))
            val reopened = model()
            advanceUntilIdle()
            assertEquals(8, reopened.week.value)
            assertEquals(today, reopened.focusedDate.value)
            assertEquals(today.dayOfWeek.ordinal, reopened.selectedDay.value)
            assertEquals("8", preferences.get(PreferenceKeys.WEEK))
        }
    }

    @Test fun refreshKeepsTemporaryNavigationAndReturningToTodayResetsIt() = runTest(dispatcher) {
        withFixture { preferences, _, model ->
            preferences.set(PreferenceKeys.WEEK, "8")
            val viewModel = model()
            advanceUntilIdle()
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val nextWeek = today.plus(DatePeriod(days = 7))
            viewModel.setWeek(9)
            viewModel.selectDate(nextWeek)
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(9, viewModel.week.value)
            assertEquals(nextWeek, viewModel.focusedDate.value)
            viewModel.returnToToday()
            assertEquals(8, viewModel.week.value)
            assertEquals(today, viewModel.focusedDate.value)
            assertEquals("8", preferences.get(PreferenceKeys.WEEK))
        }
    }

    @Test fun crossingIntoAnotherWeekUpdatesTheCalendarWithoutSavingTemporaryBrowsing() = runTest(dispatcher) {
        withFixture { preferences, _, model ->
            val today = currentLocalDate()
            AcademicWeekRepository(preferences).set(8, today)
            val viewModel = model()
            advanceUntilIdle()
            viewModel.setWeek(12)
            val nextWeek = today.plus(DatePeriod(days = 7))
            viewModel.updateDate(nextWeek)
            advanceUntilIdle()
            assertEquals(9, viewModel.currentWeek.value)
            assertEquals(9, viewModel.week.value)
            assertEquals(nextWeek, viewModel.focusedDate.value)
            assertEquals("8", preferences.get(PreferenceKeys.WEEK))
        }
    }

    private suspend fun withFixture(block: suspend (AppPreferenceRepository, MockAcademicAffairs, () -> TimetableViewModel) -> Unit) {
        val file = Files.createTempFile("timetable-navigation-", ".db")
        val database = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(dispatcher).build()
        val preferences = AppPreferenceRepository(database)
        val service = MockAcademicAffairs(0, 0)
        val session = LoggedInTimetableSession()
        val repository = TimetableRepository(service, session)
        val models = mutableListOf<TimetableViewModel>()
        try {
            block(preferences, service) {
                TimetableViewModel(repository, preferences, session,
                    TimetableCustomizationRepository(preferences), AcademicWeekRepository(preferences)).also(models::add)
            }
        } finally {
            models.forEach { it.viewModelScope.cancel() }
            dispatcher.scheduler.advanceUntilIdle()
            database.close()
            file.deleteIfExists()
        }
    }
}

private class LoggedInTimetableSession : SessionManager {
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
