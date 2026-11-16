package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgressModule
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.component.openStudyDetail
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class StudyCatalogRepositoryTest {
    private val repository = StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(0))

    @Test fun courseTimetableGroupsAllSessionsWithoutLosingClasses() = runTest {
        val catalog = repository.load(AcademicFeature.CourseTimetable, Semester.now())
        val records = catalog.groups.single().records
        assertEquals(8, records.size)
        assertEquals(160, records.sumOf { it.detail.sections.size })
        assertTrue(records.all { it.detail.sections.all { section -> section.fields.any { field -> field.label == "班级" && field.value.isNotBlank() } } })
        val route = records.first().detail
        assertEquals(route, Json.decodeFromString<StudyDetailRoute>(Json.encodeToString(route)))
    }

    @Test fun teacherListLoadsFullProfileAndPaginationRemainsAvailable() = runTest {
        val first = repository.load(AcademicFeature.Teachers, Semester.now())
        assertTrue(first.hasMore)
        val teacher = first.groups.single().records.first()
        val second = repository.load(AcademicFeature.Teachers, Semester.now(), page = 2)
        assertTrue(second.hasMore)
        assertEquals(24, (first.groups.single().records + second.groups.single().records).distinctBy { it.id }.size)
        val sections = repository.teacher(teacher.detail.teacherId!!)
        assertTrue(sections.any { it.title == "教师资料" })
        assertTrue(sections.any { it.title == "近四学期主讲课程" })
        assertTrue(sections.flatMap { it.fields }.any { it.label == "个人简介" })
        val filtered = repository.load(AcademicFeature.Teachers, Semester.now(), query = "林")
        assertEquals(listOf("林知远"), filtered.groups.single().records.map { it.title })
    }

    @Test fun planKeepsSemesterGroupingAndProgressKeepsCourseDetails() = runTest {
        val plan = repository.load(AcademicFeature.AcademicPlan, Semester.now())
        assertTrue(plan.groups.size > 1)
        assertTrue(plan.groups.all { group -> group.records.all { it.detail.summary == group.title } })
        val progress = repository.load(AcademicFeature.AcademicProgress, Semester.now())
        assertEquals(2, progress.groups.size)
        assertTrue(progress.groups.first().records.all { it.progress != null && it.detail.academicProgress?.courses?.isNotEmpty() == true })
        val unknown = AcademicProgressModule("未知要求", null, null, emptyList()).record("unknown")
        assertEquals("— / — 学分", unknown.value)
        assertNull(unknown.progress)
        assertNull(unknown.achievement!!.completed)
        assertEquals("学分", unknown.achievement.unit)
        val detail = progress.groups.first().records.first().detail
        assertEquals(24, detail.academicProgress!!.courses.size)
        assertEquals(detail, Json.decodeFromString<StudyDetailRoute>(Json.encodeToString(detail)))
    }

    @Test fun extracurricularTabsUseDifferentEndpointsAndEventsHaveDetails() = runTest {
        val grades = repository.load(AcademicFeature.Extracurricular, Semester.now(), tab = 0).groups.single()
        val events = repository.load(AcademicFeature.Extracurricular, Semester.now(), tab = 1).groups.single()
        val logs = repository.load(AcademicFeature.Extracurricular, Semester.now(), tab = 2).groups.single()
        assertEquals(7, grades.records.size)
        assertTrue(grades.records.all { it.progress != null })
        assertEquals(listOf(false, false, false, false, true, true, true), grades.records.map { it.achievement!!.completed })
        assertTrue(events.records.all { it.detail.eventId != null })
        assertTrue(events.records.all { it.tags.size == 2 && it.tags.count { tag -> tag.isScore } == 1 && it.subtitle.isBlank() })
        assertTrue(events.records.all { Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} · (线上|线下)$").matches(it.supporting) })
        val details = repository.event(events.records.first().detail.eventId!!)
        assertTrue(details.flatMap { it.fields }.any { it.label == "地点" })
        assertTrue(logs.records.isNotEmpty())
        assertTrue(logs.records.none { it.detail.eventId != null })
        assertTrue(logs.records.all { it.tags.size == 2 && it.tags.count { tag -> tag.isScore } == 1 && it.subtitle.isBlank() })
        assertTrue(logs.records.all { Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$").matches(it.supporting) })
    }

    @Test fun eventLogoIsRetainedAndRelativeUrlIsResolved() = runTest {
        val demo = MockExtracurricularActivity(0)
        val service = object : com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity by demo {
            override val activity = object : com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity.Activity by demo.activity {
                override suspend fun getEvents() = demo.activity.getEvents().map { it.copy(imageUrl = "/uploads/activity.png") }
            }
        }
        val catalog = StudyCatalogRepository(MockAcademicAffairs(0, 0), service)
            .load(AcademicFeature.Extracurricular, Semester.now(), tab = 1)
        assertTrue(catalog.groups.single().records.all { it.imageUrl == "http://2ketang.gdufe.edu.cn/uploads/activity.png" })
    }

    @Test fun selectingAnotherDetailReplacesRatherThanStacksIt() {
        val list = object : NavKey {}
        val backStack = mutableListOf<NavKey>(list)
        backStack.openStudyDetail(StudyDetailRoute("1", "First"))
        backStack.openStudyDetail(StudyDetailRoute("2", "Second"))
        assertEquals(2, backStack.size)
        assertSame(list, backStack.first())
        assertEquals("2", (backStack.last() as StudyDetailRoute).id)
    }
}
