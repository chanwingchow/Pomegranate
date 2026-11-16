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
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DayOfWeek
import com.chow.pomegranate.ui.screen.main.timetable.TimetableDisplay

@OptIn(ExperimentalCoroutinesApi::class)
class MockAcademicAffairsTest {
    @Test fun includesACompletedExamForTheEndedFilter() = runTest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val exams = MockAcademicAffairs(0, 0).enrollment.getExamSchedule(Semester.now()).exams
        val completed = exams.single { it.courseId == "DEMO-FINISHED" }
        assertTrue(completed.endTime < now)
        assertTrue(completed.startTime < completed.endTime)
        assertEquals("大学英语阶段测验", completed.courseName)
    }

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
        assertEquals(160, request.await().courses.size)
        assertTrue(request.await().courses.any { "毛泽东思想" in it.name })
    }

    @Test fun timetableIncludesStandaloneOddWeekAndAlternatingCoursesInTheSameSlot() = runTest {
        val courses = MockAcademicAffairs(0, 0).enrollment.getTimetable(Semester.now()).courses
        val odd = TimetableDisplay.of(courses, week = 1)
        val even = TimetableDisplay.of(courses, week = 2)
        assertEquals("高等数学", odd.coursesAt(2, DayOfWeek.MONDAY).single().name)
        assertTrue(even.coursesAt(2, DayOfWeek.MONDAY).isEmpty())
        assertEquals("数据库原理", odd.coursesAt(0, DayOfWeek.WEDNESDAY).single().name)
        assertEquals("计算机网络", even.coursesAt(0, DayOfWeek.WEDNESDAY).single().name)
        assertEquals(setOf("数据库原理", "计算机网络"),
            TimetableDisplay.of(courses, week = 2, includeOtherWeeks = true).coursesAt(0, DayOfWeek.WEDNESDAY).map { it.name }.toSet())
    }

    @Test
    fun selectionUpdatesSearchCreditsAndDropHistoryOnlyInCurrentInstance() = runTest {
        val api = MockAcademicAffairs(0)
        val system = api.courseSystem
        system.enterCourseSystem(system.getEntrances().single().id)
        val originalLogs = system.getCourseDropLogs()
        val initialCredits = system.getCreditSummary().progress.first { it.category == "本轮总学分" }.selected.toDouble()
        val course = system.searchCourses(CourseSearchParam.General(query = "人工智能")).items.single()
        assertFalse(system.getSelectedCourseTimetable().rows.any { row -> row.days.any { course.name in it } })
        assertTrue(system.selectCourse(course.id, CourseSelectCategory.General).isSuccess)
        assertTrue(system.getSelectedCourseTimetable().rows.any { row -> row.days.any { course.name in it } })
        assertFalse(system.selectCourse(course.id, CourseSelectCategory.General).isSuccess)
        assertEquals(initialCredits + course.credits, system.getCreditSummary().progress.first { it.category == "本轮总学分" }.selected.toDouble())
        assertTrue(system.getSelectedCourses().any { it.id == course.id })
        val selected = system.searchCourses(CourseSearchParam.General(query = "人工智能")).items.single()
        assertTrue(selected.isSelected)
        assertEquals(course.remaining - 1, selected.remaining)
        assertTrue(system.dropCourse(course.id).isSuccess)
        assertFalse(system.getSelectedCourseTimetable().rows.any { row -> row.days.any { course.name in it } })
        assertFalse(system.dropCourse(course.id).isSuccess)
        assertEquals(initialCredits, system.getCreditSummary().progress.first { it.category == "本轮总学分" }.selected.toDouble())
        assertEquals(originalLogs.size + 1, system.getCourseDropLogs().size)
        assertEquals(course.name, system.getCourseDropLogs().first().courseName)
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
        assertTrue(api.enrollment.getTeachers(pageIndex = 2).hasMorePages)
        assertFalse(api.enrollment.getTeachers(pageIndex = 6).hasMorePages)
        val teacher = api.enrollment.getTeachers("林").items.single()
        assertEquals(teacher.name, api.enrollment.getTeacher(teacher.id).name)
        assertTrue(api.enrollment.getTeachers("不存在").items.isEmpty())
        val page = api.courseSystem.searchCourses(CourseSearchParam.General(pageIndex = 1, pageSize = 1))
        assertTrue(page.filteredCount >= 40)
        assertEquals(1, page.items.size)
        assertEquals("人工智能导论", page.items.single().name)
        val filtered = api.courseSystem.searchCourses(CourseSearchParam.General(query = "林知远", pageSize = 1))
        assertTrue(filtered.filteredCount >= 2)
        assertEquals("人工智能导论", filtered.items.single().name)
        val byTeacher = api.courseSystem.searchCourses(CourseSearchParam.General(teacher = "林知远"))
        val byCourseName = api.courseSystem.searchCourses(CourseSearchParam.General(teacher = "人工智能"))
        assertEquals(filtered.filteredCount, byTeacher.filteredCount)
        assertEquals(filtered.items.first(), byTeacher.items.first())
        assertEquals("人工智能导论", byCourseName.items.single().name)
    }

    @Test fun previewListsAreLongAndFiltersMatchTheExpandedCatalog() = runTest {
        val api = MockAcademicAffairs(0, 0)
        assertTrue(api.courseSystem.getSelectedCourses().size >= 18)
        assertTrue(api.courseSystem.getCourseDropLogs().size >= 30)
        assertTrue(api.enrollment.getCourseTranscript().items.size >= 40)
        assertTrue(api.enrollment.getLevelTranscript().items.size >= 15)
        assertTrue(api.academic.getAcademicPlan().courses.groupBy { it.semester }.values.all { it.size >= 20 })
        val all = api.courseSystem.searchCourses(CourseSearchParam.General(pageSize = 100)).items
        assertTrue(all.any { it.isFull })
        assertTrue(all.any { it.conflictDescription.isNotBlank() })
        assertTrue(all.any { !it.isOpenForSelection })
        val filtered = api.courseSystem.searchCourses(CourseSearchParam.General(filterFull = true, filterConflicts = true, pageSize = 100))
        assertTrue(filtered.items.all { !it.isFull && it.conflictDescription.isBlank() })
        val foshan = api.courseSystem.searchCourses(CourseSearchParam.General(campus = com.chow.pomegranate.service.foundation.Campus.Foshan, pageSize = 100))
        assertTrue(foshan.items.isNotEmpty() && foshan.items.all { it.campusId == "1" })
        val closed = all.first { !it.isOpenForSelection }
        assertFalse(api.courseSystem.selectCourse(closed.id, CourseSelectCategory.General).isSuccess)
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
