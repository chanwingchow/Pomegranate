package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDateTime
import kotlinx.coroutines.test.runTest
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.Section
import com.chow.pomegranate.service.foundation.Campus
import kotlinx.datetime.DayOfWeek
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class CourseSelectionRepositoryTest {
    @Test
    fun forwardsAllSupportedFiltersAndKeepsBasicCategoriesUnfiltered() = runTest {
        val demo = MockAcademicAffairs(0, 0)
        var received: CourseSearchParam? = null
        val service = object : AcademicAffairs by demo {
            override val courseSystem = object : AcademicAffairs.CourseSystem by demo.courseSystem {
                override suspend fun searchCourses(params: CourseSearchParam) = demo.courseSystem.searchCourses(params).also { received = params }
            }
        }
        val repository = CourseSelectionRepository(service)
        val filters = CourseSearchFilters(" 张老师 ", 3, Section.Second, Campus.Foshan, false, false)
        CourseSelectCategory.entries.forEach { category ->
            repository.searchPage(" 课程 ", category, page = 2, filters = filters)
            val param = received!!
            assertEquals(2, param.pageIndex)
            assertEquals(20, param.pageSize)
            if (param is CourseSearchParam.CourseSearchExtraParam) {
                assertEquals("课程", param.query)
                assertEquals("张老师", param.teacher)
                assertEquals(DayOfWeek.WEDNESDAY, param.dayOfWeek)
                assertEquals(Section.Second, param.section)
                assertEquals(false, param.filterFull)
                assertEquals(false, param.filterConflicts)
            }
            if (param is CourseSearchParam.General) assertEquals(Campus.Foshan, param.campus)
        }
        assertEquals(null, filters.forCategory(CourseSelectCategory.Professional).campus)
        assertEquals(CourseSearchFilters(), filters.forCategory(CourseSelectCategory.Basic))
    }

    @Test
    fun eachCategorySearchesAndSelectsUsingItsOwnRoute() = runTest {
        val repository = CourseSelectionRepository(MockAcademicAffairs(0, 0))
        CourseSelectCategory.entries.forEach { category ->
            val page = repository.searchPage("", category)
            assertTrue(page.items.isNotEmpty())
            val course = page.items.first { !it.isSelected }
            assertTrue(repository.select(course.id, category).isSuccess)
            assertTrue(repository.searchPage("", category).items.first { it.id == course.id }.isSelected)
            assertTrue(repository.drop(course.id).isSuccess)
        }
        val first = repository.searchPage("", CourseSelectCategory.General)
        val next = repository.searchPage("", CourseSelectCategory.General, page = 1)
        assertEquals(first.filteredCount, next.filteredCount)
        assertTrue(next.items.isNotEmpty())
        assertTrue(first.items.map { it.id }.toSet().intersect(next.items.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun latestEntranceUsesEndTime() {
        val semester = Semester(2025, Semester.Term.First)
        val entrances = listOf(
            CourseSystemEntrance(
                id = "old",
                userId = "student",
                semester = semester,
                name = "第一轮",
                startTime = LocalDateTime(2025, 8, 1, 9, 0),
                endTime = LocalDateTime(2025, 8, 5, 17, 0),
            ),
            CourseSystemEntrance(
                id = "latest",
                userId = "student",
                semester = semester,
                name = "第二轮",
                startTime = LocalDateTime(2025, 8, 6, 9, 0),
                endTime = LocalDateTime(2025, 8, 10, 17, 0),
            ),
        )

        assertEquals("latest", entrances.latestCourseSystemEntrance()?.id)
    }
}
