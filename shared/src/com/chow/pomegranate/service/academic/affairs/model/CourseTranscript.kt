package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester

/**
 * 课程成绩。
 *
 * @property userId 学号
 * @property overview 概览
 * @property items 课程成绩项
 */
data class CourseTranscript(
    val userId: String,
    val overview: String,
    val items: List<CourseGrade>,
)

/**
 * 课程成绩项。
 *
 * @property semester 学期
 * @property courseId 课程编号
 * @property name 课程名称
 * @property regularScore 平时成绩
 * @property labScore 实验成绩
 * @property finalScore 期末成绩
 * @property score 成绩
 * @property credits 学分
 * @property hours 学时
 * @property assessment 考核方式
 * @property attribute 课程属性
 * @property category 课程性质
 * @property electiveCategory 通选课分类
 * @property examCategory 考试性质
 * @property mark 成绩标识
 * @property note 备注
 */
data class CourseGrade(
    val semester: Semester,
    val courseId: String,
    val name: String,
    val regularScore: String?,
    val labScore: String?,
    val finalScore: String?,
    val score: String,
    val credits: Double,
    val hours: Double,
    val assessment: String,
    val attribute: String,
    val category: String,
    val electiveCategory: String?,
    val examCategory: String,
    val mark: String?,
    val note: String?,
)