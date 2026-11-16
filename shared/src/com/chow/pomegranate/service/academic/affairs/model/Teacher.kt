package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester

/**
 * 教师。
 *
 * @property id 工号
 * @property name 姓名
 * @property gender 性别
 * @property politics 政治面貌
 * @property nation 民族
 * @property duty 职务
 * @property title 职称
 * @property staffCategory 教职工类别
 * @property department 部门（院系）
 * @property office 科室（系）
 * @property qualification 最高学历
 * @property degree 学位
 * @property researchField 研究方向
 * @property phoneNumber 手机号
 * @property qq QQ
 * @property weChat 微信
 * @property email 邮箱
 * @property biography 个人简介
 * @property recentCourses 近四个学期主讲课程
 * @property plannedCourses 下学期计划开设课程
 * @property philosophy 教育理念
 * @property slogan 最想对学生说的话
 */
data class Teacher(
    val id: String,
    val name: String,
    val gender: String?,
    val politics: String?,
    val nation: String?,
    val duty: String?,
    val title: String?,
    val staffCategory: String?,
    val department: String?,
    val office: String?,
    val qualification: String?,
    val degree: String?,
    val researchField: String?,
    val phoneNumber: String?,
    val qq: String?,
    val weChat: String?,
    val email: String?,
    val biography: String?,
    val recentCourses: List<TeacherCourse>,
    val plannedCourses: List<TeacherCourse>,
    val philosophy: String?,
    val slogan: String?,
)

/**
 * 教师课程。
 *
 * @property name 课程名称
 * @property category 课程类别
 * @property semester 学期
 */
data class TeacherCourse(
    val name: String,
    val category: String,
    val semester: Semester,
)