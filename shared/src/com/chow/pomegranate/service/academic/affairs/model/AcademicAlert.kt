package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester

/**
 * 学业预警。
 *
 * @param userId 学号
 * @property pageIndex 当前页码
 * @property pageCount 总页数
 * @param items 预警项
 */
data class AcademicAlert(
    val userId: String,
    val pageIndex: Int,
    val pageCount: Int,
    val items: List<AcademicAlertItem>,
)

/**
 * 学业预警项。
 *
 * @property semester 学期
 * @property level 预警等级
 * @property description 预警描述
 * @property actualValue 实际值
 */
data class AcademicAlertItem(
    val semester: Semester,
    val level: AcademicAlertLevel,
    val description: String,
    val actualValue: String,
)

/**
 * 学业预警等级。
 */
enum class AcademicAlertLevel {
    /** 高（红色） */
    High,
    /** 中（黄色） */
    Medium,
    /** 低（蓝色） */
    Low,
}