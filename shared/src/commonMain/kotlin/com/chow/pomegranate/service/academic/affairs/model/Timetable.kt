package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.DayOfWeek

/**
 * 课表。
 *
 * @property semester 学期
 * @property courses 课程
 * @property note 备注
 */
data class Timetable(
    val semester: Semester,
    val courses: List<TimetableCourse>,
    val note: String?,
)

/**
 * 课表课程。
 *
 * @property name 课程名称
 * @property teacher 教师
 * @property weeks 上课周次，如 `1-16周(单)`
 * @property classroom 上课地点
 * @property dayOfWeek 星期
 * @property sections 节次
 */
data class TimetableCourse(
    val name: String,
    val teacher: String?,
    val weeks: String,
    val classroom: String?,
    val dayOfWeek: DayOfWeek,
    val sections: Set<Int>,
)