package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDateTime

/**
 * 考试安排。
 *
 * @property userId 学号
 * @property semester 学期
 * @property exams 考试
 */
data class ExamSchedule(
    val userId: String,
    val semester: Semester,
    val exams: List<CourseExam>,
)

/**
 * 课程考试。
 *
 * @property courseId 课程编号
 * @property courseName 课程名称
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property campus 校区
 * @property classroom 考场
 */
data class CourseExam(
    val courseId: String,
    val courseName: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val campus: Campus,
    val classroom: String,
)