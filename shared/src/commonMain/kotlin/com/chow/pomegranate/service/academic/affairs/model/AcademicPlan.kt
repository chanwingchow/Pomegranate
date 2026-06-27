package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester

/**
 * 学业计划。
 *
 * @property userId 学号
 * @property courses 计划课程
 */
data class AcademicPlan(
    val userId: String,
    val courses: List<PlannedCourse>,
)

/**
 * 计划课程。
 *
 * @property semester 学期
 * @property courseId 课程编号
 * @property name 课程名称
 * @property department 开课单位
 * @property credits 学分
 * @property hours 总学时
 * @property assessment 考核方式
 * @property attribute 课程属性，选修、必修
 * @property isExamAssessed 是否考试
 */
data class PlannedCourse(
    val semester: Semester,
    val courseId: String,
    val name: String,
    val department: String,
    val credits: Double,
    val hours: Int,
    val assessment: String,
    val attribute: String,
    val isExamAssessed: Boolean,
)