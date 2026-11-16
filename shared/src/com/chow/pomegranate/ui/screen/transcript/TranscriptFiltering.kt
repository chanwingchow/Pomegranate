package com.chow.pomegranate.ui.screen.transcript

import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.academic.affairs.model.LevelGrade

/** Filters locally cached course grades by the fields students can identify. */
internal fun List<CourseGrade>.filterCourseGradesByKeyword(keyword: String): List<CourseGrade> {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return this
    return filter { grade ->
        listOf(grade.name, grade.courseId, grade.semester.toString(), grade.score, grade.attribute)
            .any { it.contains(normalized, ignoreCase = true) }
    }
}

/** Filters locally cached level-exam results without another transcript request. */
internal fun List<LevelGrade>.filterLevelGradesByKeyword(keyword: String): List<LevelGrade> {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return this
    return filter { grade ->
        listOf(grade.name, grade.score, grade.date.toString())
            .any { it.contains(normalized, ignoreCase = true) }
    }
}
