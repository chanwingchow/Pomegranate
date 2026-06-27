package com.chow.pomegranate.service.academic.affairs.model

/**
 * 教师列表。
 *
 * @property pageIndex 当前页码
 * @property pageCount 总页数
 * @property items 教师项
 */
data class Teachers(
    val pageIndex: Int,
    val pageCount: Int,
    val items: List<BasicTeacher>,
)

/**
 * 教师项。
 *
 * @property id 工号
 * @property name 姓名
 * @property department 部门（院系）
 */
data class BasicTeacher(
    val id: String,
    val name: String,
    val department: String?,
)