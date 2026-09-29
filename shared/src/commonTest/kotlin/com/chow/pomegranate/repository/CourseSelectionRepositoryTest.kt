package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CourseSelectionRepositoryTest {
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
