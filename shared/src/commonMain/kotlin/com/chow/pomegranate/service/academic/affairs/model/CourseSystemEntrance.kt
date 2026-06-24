package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDateTime

/**
 * 选课系统入口。
 *
 * @property id 系统 id
 * @property userId 用户
 * @property semester 学期
 * @property name 选课名称
 * @property startTime 开始时间
 * @property endTime 结束时间
 */
data class CourseSystemEntrance(
    val id: String,
    val userId: String,
    val semester: Semester,
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
)