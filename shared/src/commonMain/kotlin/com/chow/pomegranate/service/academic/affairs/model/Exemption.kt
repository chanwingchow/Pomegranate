package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDate

/**
 * 免听申请。
 *
 * @property userId 学号
 * @property semester 学期
 * @property items 免听申请项
 */
data class Exemption(
    val userId: String,
    val semester: Semester,
    val items: List<ExemptionItem>,
)

/**
 * 免听申请项。
 *
 * @property courseId 课程编号
 * @property courseName 课程名称
 * @property department 开课单位
 * @property teacher 教师
 * @property hours 学时
 * @property credits 学分
 * @property assessment 考核方式
 * @property reason 免听原因
 * @property status 审核状态
 * @property appliedDate 申请时间
 * @property applyUrl 申请链接
 */
data class ExemptionItem(
    val courseId: String,
    val courseName: String,
    val department: String,
    val teacher: String,
    val hours: Int,
    val credits: Double,
    val assessment: String,
    val reason: String?,
    val status: String?,
    val appliedDate: LocalDate?,
    val applyUrl: String?,
)