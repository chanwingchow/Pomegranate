package com.chow.pomegranate.ui.screen.transcript

import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.foundation.Semester

internal data class GradeAttempt(val id: String, val grade: CourseGrade)
internal data class CourseGradeGroup(val id: String, val attempts: List<GradeAttempt>) {
    val latest: CourseGrade get() = attempts.last().grade
    val hasFailure: Boolean get() = attempts.any { it.grade.isFailed }
}

/** 未出分、缓考等状态不当作挂科；文字等级与百分制分别判断。 */
internal val CourseGrade.isFailed: Boolean get() {
    val value = score.trim().replace("分", "").trim()
    return value.toDoubleOrNull()?.let { it < 60 } ?: (value in setOf("不及格", "不合格", "未通过", "不通过", "缺考", "旷考", "作弊"))
}

internal fun List<CourseGrade>.defaultTranscriptSemester(current: Semester = Semester.now()): String? =
    current.toString().takeIf { term -> any { it.semester.toString() == term } }

/** 先按课程编号整合，再筛选课程，始终保留该课程的完整考试历史。 */
internal fun List<CourseGrade>.courseGradeGroups(keyword: String = "", semester: String? = null, failedOnly: Boolean = false): List<CourseGradeGroup> {
    val occurrences = mutableMapOf<String, Int>()
    val attempts = map { grade ->
        val base = "${grade.courseId.ifBlank { "name:${grade.name}" }}:${grade.semester}:${grade.examCategory}"
        val occurrence = occurrences.getOrElse(base) { 0 }
        occurrences[base] = occurrence + 1
        GradeAttempt("$base:$occurrence", grade)
    }
    return attempts.groupBy { it.grade.courseId.trim().ifBlank { "name:${it.grade.name}" } }
        .map { (id, records) -> CourseGradeGroup(id, records.sortedWith(compareBy<GradeAttempt> { it.grade.semester }
            .thenBy { if (it.grade.examCategory.contains("补") || it.grade.examCategory.contains("重")) 1 else 0 })) }
        .filter { group ->
            (semester == null || group.attempts.any { it.grade.semester.toString() == semester }) &&
                (!failedOnly || group.hasFailure) && group.attempts.map { it.grade }.filterCourseGradesByKeyword(keyword).isNotEmpty()
        }
        .sortedByDescending { it.latest.semester }
}
