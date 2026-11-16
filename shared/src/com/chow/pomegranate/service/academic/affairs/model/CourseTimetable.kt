package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester

/**
 * 课程课表。
 *
 * @property semester 学期
 * @property courses 课程
 */
data class CourseTimetable(
    val semester: Semester,
    val courses: List<TimetableCourse>,
)