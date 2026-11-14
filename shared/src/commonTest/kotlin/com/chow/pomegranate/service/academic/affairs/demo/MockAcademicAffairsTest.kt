package com.chow.pomegranate.service.academic.affairs.demo

import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MockAcademicAffairsTest {
    @Test
    fun respondsWithDelayAndHonorsRequestedSemester() = runTest {
        val api = MockAcademicAffairs()
        val semester = Semester(2025, Semester.Term.Second)
        val result = async { api.enrollment.getTimetable(semester) }
        runCurrent()
        assertFalse(result.isCompleted)
        advanceTimeBy(349)
        assertFalse(result.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(semester, result.await().semester)
        assertEquals(MockAcademicAffairs.USER_ID, result.await().userId)
        assertTrue(result.await().courses.isNotEmpty())
        assertEquals(350L, currentTime)
    }

    @Test
    fun schoolTimetableTakesOneAndAHalfSecondsAndContainsMoreCourses() = runTest {
        val api = MockAcademicAffairs()
        val request = async { api.enrollment.getCourseTimetable(Semester.now()) }
        runCurrent()
        advanceTimeBy(1_499)
        assertFalse(request.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1_500L, currentTime)
        assertEquals(120, request.await().courses.size)
    }

    @Test
    fun selectionUpdatesSearchCreditsAndDropHistoryOnlyInCurrentInstance() = runTest {
        val api = MockAcademicAffairs(0)
        val system = api.courseSystem
        system.enterCourseSystem(system.getEntrances().single().id)
        val initialCredits = system.getCreditSummary().progress.single().selected.toDouble()
        val course = system.searchCourses(CourseSearchParam.General(query = "人工智能")).items.single()
        assertTrue(system.selectCourse(course.id, CourseSelectCategory.General).isSuccess)
        assertFalse(system.selectCourse(course.id, CourseSelectCategory.General).isSuccess)
        assertEquals(initialCredits + course.credits, system.getCreditSummary().progress.single().selected.toDouble())
        assertTrue(system.getSelectedCourses().any { it.id == course.id })
        val selected = system.searchCourses(CourseSearchParam.General(query = "人工智能")).items.single()
        assertTrue(selected.isSelected)
        assertEquals(course.remaining - 1, selected.remaining)
        assertTrue(system.dropCourse(course.id).isSuccess)
        assertFalse(system.dropCourse(course.id).isSuccess)
        assertEquals(initialCredits, system.getCreditSummary().progress.single().selected.toDouble())
        assertEquals(course.name, system.getCourseDropLogs().single().courseName)
        assertTrue(system.selectCourse(course.id, CourseSelectCategory.General).isSuccess)
        assertFalse(MockAcademicAffairs(0).courseSystem.getSelectedCourses().any { it.id == course.id })
    }

    @Test
    fun cancellingDelayedSelectionDoesNotChangeData() = runTest {
        val system = MockAcademicAffairs().courseSystem
        val before = system.getSelectedCourses()
        val request = async { system.selectCourse("demo-select-2", CourseSelectCategory.General) }
        runCurrent()
        advanceTimeBy(100)
        request.cancelAndJoin()
        assertEquals(before, system.getSelectedCourses())
    }

    @Test
    fun teacherSearchAndCoursePaginationApplyFiltersBeforePaging() = runTest {
        val api = MockAcademicAffairs(0)
        assertTrue(api.enrollment.getTeachers(pageIndex = 1).hasMorePages)
        assertFalse(api.enrollment.getTeachers(pageIndex = 2).hasMorePages)
        val teacher = api.enrollment.getTeachers("林").items.single()
        assertEquals(teacher.name, api.enrollment.getTeacher(teacher.id).name)
        assertTrue(api.enrollment.getTeachers("不存在").items.isEmpty())
        val page = api.courseSystem.searchCourses(CourseSearchParam.General(pageIndex = 1, pageSize = 1))
        assertEquals(3, page.filteredCount)
        assertEquals(1, page.items.size)
        assertEquals("人工智能导论", page.items.single().name)
        val filtered = api.courseSystem.searchCourses(CourseSearchParam.General(query = "林知远", pageSize = 1))
        assertEquals(1, filtered.filteredCount)
        assertEquals("人工智能导论", filtered.items.single().name)
    }

    @Test
    fun allReadModulesHaveUsableOfflineResponses() = runTest {
        val api = MockAcademicAffairs(0)
        val semester = Semester.now()
        assertTrue(api.auth.getCaptcha().isNotEmpty())
        assertEquals(LoginResult.Success, api.auth.login(LoginParam("", "", "")))
        api.auth.logout()
        assertTrue(api.enrollment.getCourseTimetable(semester).courses.isNotEmpty())
        assertEquals(semester, api.enrollment.getExamSchedule(semester).semester)
        assertTrue(api.enrollment.getCourseTranscript().items.isNotEmpty())
        assertTrue(api.enrollment.getLevelTranscript().items.isNotEmpty())
        assertTrue(api.enrollment.getExemption(semester).items.isNotEmpty())
        assertEquals(semester, api.enrollment.getCampusCalendar(semester).semester)
        assertTrue(api.academic.getAcademicPlan().courses.isNotEmpty())
        assertFalse(api.academic.getAcademicProgress(false).isPrimary)
        assertTrue(api.academic.getAcademicAlert().items.isNotEmpty())
        assertTrue(api.academic.getAcademicProgram().html.contains("演示"))
        val report = api.academic.getGraduationAudit().items.single().reportUrlString
        assertTrue(api.academic.getGraduationAuditReport(report).decodeToString().startsWith("%PDF-1.4"))
    }
}
