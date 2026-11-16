package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StudyCatalogFiltersStateTest {
    @Test fun returningAndRefreshingKeepLocalFiltersAndQueryWithoutRefetchingOnEveryEdit() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val demo = MockAcademicAffairs(0, 0)
        var calls = 0
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getCourseTimetable(semester: Semester) = demo.enrollment.getCourseTimetable(semester).also { calls++ }
            }
        }
        val session = SchoolFilterSession()
        val model = StudyCatalogViewModel(StudyCatalogRepository(service, MockExtracurricularActivity(0)), session, testSchoolTimetables(session, service))
        try {
            model.ensureLoaded(AcademicFeature.CourseTimetable)
            advanceUntilIdle()
            val raw = model.state.value.catalog
            val filters = SchoolTimetableFilters(teacher = "林知远", clazz = "1 班", weekday = 1, week = 3)
            model.setSchoolTimetableFilters(filters)
            model.setLocalQuery("数据")
            model.ensureLoaded(AcademicFeature.CourseTimetable)
            advanceUntilIdle()
            assertEquals(1, calls)
            assertSame(raw, model.state.value.catalog)
            model.refresh()
            advanceUntilIdle()
            assertEquals(2, calls)
            assertEquals(filters, model.schoolTimetableFilters.value)
            assertEquals("数据", model.state.value.query)
            model.load(AcademicFeature.CourseTimetable, semester = model.state.value.semester.next())
            advanceUntilIdle()
            assertEquals(filters, model.schoolTimetableFilters.value)
            assertEquals("数据", model.state.value.query)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}

private class SchoolFilterSession : SessionManager {
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
