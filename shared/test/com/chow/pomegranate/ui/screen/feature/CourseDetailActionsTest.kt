package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.ui.component.AdaptiveStudyDetailRoute
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class CourseDetailActionsTest {
    @Test fun selectedSearchCourseOffersDropEvenWhenSelectionIsClosed() = runTest {
        val api = MockAcademicAffairs(0, 0)
        val course = api.courseSystem.searchCourses(CourseSearchParam.General()).items.first()
        val selected = course.copy(isSelected = true, isOpenForSelection = false, remaining = 0)
        val action = selected.record(CourseSelectCategory.General).detail.action!!
        assertTrue(action.drop)
        assertEquals(course.id, action.id)
        assertEquals(course.courseId, action.courseCode)
        val blocked = course.copy(isSelected = false, isOpenForSelection = false).record(CourseSelectCategory.General)
        assertNull(blocked.detail.action)
    }

    @Test fun closedCoursesRemainClosedInPreferenceModeAndLogsHaveNoAction() = runTest {
        val api = MockAcademicAffairs(0, 0)
        val raw = api.courseSystem.searchCourses(CourseSearchParam.General()).items.first()
        assertNull(raw.copy(isSelected = false, isOpenForSelection = false).record(CourseSelectCategory.General, true).detail.action)
        val log = api.courseSystem.getCourseDropLogs().first().detailRoute()
        assertNull(log.action)
        assertTrue(log.sections.flatMap { it.fields }.any { it.label == "退课时间" })
        val route = AdaptiveStudyDetailRoute(log)
        assertEquals(route, Json.decodeFromString<AdaptiveStudyDetailRoute>(Json.encodeToString(route)))
    }
}
