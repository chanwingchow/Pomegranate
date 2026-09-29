package com.chow.pomegranate.ui.screen.transcript

import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.academic.affairs.model.LevelGrade
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptFilteringTest {
    @Test
    fun courseGradesMatchNameCodeSemesterAndScore() {
        val grade = CourseGrade(
            semester = Semester(2024, Semester.Term.First), courseId = "CS101", name = "程序设计",
            regularScore = null, labScore = null, finalScore = null, score = "92",
            credits = 3.0, hours = 48.0, assessment = "考试", attribute = "必修",
            category = "专业课", electiveCategory = null, examCategory = "正常", mark = null, note = null,
        )
        assertEquals(listOf(grade), listOf(grade).filterCourseGradesByKeyword("cs101"))
        assertEquals(listOf(grade), listOf(grade).filterCourseGradesByKeyword("92"))
        assertEquals(emptyList(), listOf(grade).filterCourseGradesByKeyword("不存在"))
    }

    @Test
    fun levelGradesMatchExamNameAndDate() {
        val grade = LevelGrade("大学英语四级", "560", LocalDate(2025, 6, 14))
        assertEquals(listOf(grade), listOf(grade).filterLevelGradesByKeyword("2025-06"))
        assertEquals(listOf(grade), listOf(grade).filterLevelGradesByKeyword("英语"))
    }
}
