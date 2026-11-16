package com.chow.pomegranate.ui.screen.transcript

import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TranscriptGroupingTest {
    private val current = Semester(2026, Semester.Term.First)
    private fun grade(id: String = "CS101", name: String = "程序设计", score: String = "54", term: Semester = current.previous(), category: String = "正常考试") =
        CourseGrade(term, id, name, null, null, score, score, 3.0, 48.0, "考试", "必修", "专业课", null, category, null, null)

    @Test fun groupsByCodeAndKeepsFailedAndSuccessfulAttemptsAcrossSemesters() {
        val fail = grade()
        val retake = grade(name = "程序设计（新名称）", score = "82", term = current, category = "重修")
        val sameNameDifferentCode = grade(id = "CS102", score = "96", term = current)
        val groups = listOf(fail, retake, sameNameDifferentCode).courseGradeGroups("CS101", current.toString(), failedOnly = true)
        assertEquals(1, groups.size)
        assertEquals(listOf(fail, retake), groups.single().attempts.map { it.grade })
        assertEquals(retake.name, groups.single().latest.name)
        assertNotEquals(groups.single().attempts[0].detailRoute().detail.id, groups.single().attempts[1].detailRoute().detail.id)
        assertEquals("54", groups.single().attempts[0].detailRoute().score)
        assertFalse(groups.single().attempts[1].detailRoute().failed)
        assertEquals(2, listOf(fail, retake, sameNameDifferentCode).courseGradeGroups().size)
    }

    @Test fun failedFilterMatchesNumericAndTextScoresWithoutTreatingPendingAsFailure() {
        listOf("59.5", "59 分", "不及格", "不合格", "未通过", "缺考").forEach { assertTrue(grade(score = it).isFailed, it) }
        listOf("60", "100", "及格", "合格", "通过", "待录入", "缓考", "", "优秀").forEach { assertFalse(grade(score = it).isFailed, it) }
        assertEquals(emptyList(), listOf(grade(score = "90")).courseGradeGroups(failedOnly = true))
    }

    @Test fun semesterDefaultsToCurrentOnlyWhenPresentAndUnknownCodesDoNotMergeUnrelatedCourses() {
        assertNull(listOf(grade()).defaultTranscriptSemester(current))
        assertNull(emptyList<CourseGrade>().defaultTranscriptSemester(current))
        assertEquals(current.toString(), listOf(grade(term = current)).defaultTranscriptSemester(current))
        val unknown = listOf(grade(id = "", name = "数学"), grade(id = "", name = "英语"))
        assertEquals(2, unknown.courseGradeGroups().size)
        val duplicate = listOf(grade(), grade())
        assertEquals(2, duplicate.courseGradeGroups().single().attempts.map { it.id }.distinct().size)
    }

    @Test fun demoIncludesCurrentSemesterFailuresAndSeparateRetakeDetails() = runTest {
        val grades = MockAcademicAffairs(0, 0).enrollment.getCourseTranscript().items
        assertEquals(Semester.now().toString(), grades.defaultTranscriptSemester())
        val groups = grades.courseGradeGroups(failedOnly = true)
        assertTrue(groups.size >= 4)
        assertTrue(groups.any { it.attempts.any { a -> a.grade.isFailed } && it.attempts.any { a -> !a.grade.isFailed } })
        assertTrue(groups.any { it.attempts.size == 2 && it.attempts.all { a -> a.grade.isFailed } })
    }
}
