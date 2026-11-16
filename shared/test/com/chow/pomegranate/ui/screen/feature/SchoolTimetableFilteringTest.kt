package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.toStudyCatalog
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.json.Json
import kotlin.test.*

class SchoolTimetableFilteringTest {
    private val monday = TimetableCourse("数据结构", "张老师", "1-16周(单)", "实验楼 A101", DayOfWeek.MONDAY, setOf(1, 2), "计算机 1 班")
    private val tuesday = monday.copy(teacher = "李老师", weeksString = "2-16周(双)", classroom = "实验楼 B202", dayOfWeek = DayOfWeek.TUESDAY,
        sections = setOf(3, 4), clazz = "计算机 2 班")
    private val all = listOf(monday, tuesday, monday.copy(name = "软件工程", classroom = null, teacher = null, clazz = null, sections = setOf(2, 3)))

    @Test fun combinedConditionsMustMatchTheSameSession() {
        assertTrue(all.filterSchoolTimetable("", SchoolTimetableFilters(teacher = "张", clazz = "2 班")).isEmpty())
        assertTrue(all.filterSchoolTimetable("A101", SchoolTimetableFilters(weekday = 2)).isEmpty())
        assertEquals(listOf(tuesday), all.filterSchoolTimetable("数据", SchoolTimetableFilters(teacher = "李", classroom = "b202", clazz = "2 班", weekday = 2)))
    }

    @Test fun teachingWeekIsOneBasedAndSectionFiltersIncludeOverlappingSessions() {
        assertEquals(listOf(monday), all.filterSchoolTimetable("数据", SchoolTimetableFilters(week = 1)))
        assertEquals(listOf(tuesday), all.filterSchoolTimetable("数据", SchoolTimetableFilters(week = 2)))
        assertTrue(all.filterSchoolTimetable("数据", SchoolTimetableFilters(week = 17)).isEmpty())
        assertEquals(listOf(tuesday, all.last()), all.filterSchoolTimetable("", SchoolTimetableFilters(section = Section.Second)))
    }

    @Test fun groupedDetailsContainOnlyMatchingSessionsAndKeepTheOriginalOptions() {
        val source = CourseTimetable(Semester.now(), all)
        val matching = all.filterSchoolTimetable("1 班", SchoolTimetableFilters(weekday = 1))
        val catalog = source.toStudyCatalog(matching)
        assertSame(source, catalog.schoolTimetable)
        val record = catalog.groups.single().records.single()
        assertEquals("1 个课次", record.supporting)
        assertEquals("张老师", record.subtitle)
        assertEquals("计算机 1 班", record.detail.sections.single().fields.single { it.label == "班级" }.value)
        val options = SchoolTimetableFilterOptions.from(catalog.schoolTimetable!!.courses)
        assertEquals(listOf("李老师", "张老师").sorted(), options.teachers)
        assertEquals(2, options.classes.size)
        assertEquals(16, options.maxWeek)
    }

    @Test fun clearingFiltersRestoresAllSessionsAndNormalizesDrafts() {
        val dirty = SchoolTimetableFilters(teacher = " 张 ", classroom = " a101 ", weekday = 99, week = 0)
        assertEquals(SchoolTimetableFilters(teacher = "张", classroom = "a101"), dirty.normalized())
        assertEquals(all, all.filterSchoolTimetable("", SchoolTimetableFilters()))
        assertFalse(SchoolTimetableFilters(teacher = "  ").isActive)
        assertTrue(all.filterSchoolTimetable("", SchoolTimetableFilters(teacher = "不存在")).isEmpty())
    }

    @Test fun filterDestinationCanRestoreItsDraftAndOptions() {
        val route = SchoolTimetableFiltersRoute(SchoolTimetableFilters("张", "A101", "1 班", 1, Section.First, 3), SchoolTimetableFilterOptions.from(all))
        assertEquals(route, Json.decodeFromString<SchoolTimetableFiltersRoute>(Json.encodeToString(route)))
    }
}
