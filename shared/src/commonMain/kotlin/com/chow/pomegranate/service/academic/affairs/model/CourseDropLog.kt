package com.chow.pomegranate.service.academic.affairs.model

/**
 * 退课记录。
 *
 * @property courseId 课程编号
 * @property courseName 课程名称
 * @property credits 学分
 * @property courseAttribute 课程属性
 * @property teacher 教师
 * @property schedule 上课时间
 * @property selectionCategory 课程分类
 * @property dropType 退课类型
 * @property dropTime 退课时间
 * @property operator 操作者
 * @property description 操作说明
 */
data class CourseDropLog(
    val courseId: String,
    val courseName: String,
    val credits: Double,
    val courseAttribute: String,
    val teacher: String,
    val schedule: List<String>,
    val selectionCategory: String,
    val dropType: String,
    val dropTime: String,
    val operator: String,
    val description: String,
)