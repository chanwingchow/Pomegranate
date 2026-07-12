package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.shared.IntBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 选课搜索响应。
 *
 * @property items 可选课程列表
 * @property echo DataTables 回显参数
 * @property totalCount 总记录数
 * @property filteredCount 过滤后记录数
 */
@Serializable
data class CourseSearchResponse(
    @SerialName("aaData")
    val items: List<SelectableCourse>,
    @SerialName("sEcho")
    val echo: String?,
    @SerialName("iTotalRecords")
    val totalCount: Int,
    @SerialName("iTotalDisplayRecords")
    val filteredCount: Int,
)

/**
 * 可选课程。
 *
 * 对应选课搜索表格中的一行；同一 [courseId] 可能有多条记录（不同时间、地点）。
 *
 * @property id 教学班 id，选课/退课操作参数
 * @property courseId 课程编号
 * @property name 课程名称
 * @property credits 学分
 * @property totalHours 总学时
 * @property teacher 教师
 * @property teacherIds 教师工号，逗号分隔
 * @property schedule 上课时间，如 "1-16周 星期五 3-4节"
 * @property classroom 上课地点
 * @property campusId 校区 id，1=佛山，2=广州
 * @property schedules 结构化排课明细
 * @property capacity 课堂人数上限
 * @property remaining 剩余名额
 * @property enrolled 已选人数
 * @property preselected 初选人数
 * @property scheduledCapacity 排课人数
 * @property selectionCategory 选课分类，如 "通识选修课程"
 * @property selectionCategoryCode 选课分类代码
 * @property attributeCode 课程属性代码
 * @property department 开课学院
 * @property offeringUnitCode 开课单位代码
 * @property conflictDescription 选课冲突说明
 * @property note 备注，如 "拟开课时间:"
 * @property isOpenForSelection 是否开放选课
 * @property isSelected 当前用户是否已选
 * @property isDeleted 是否已删除
 * @property semesterId 学年学期，如 "2024-2025-2"
 * @property courseMasterId 课程库 id
 * @property offeringCode 开课代码
 * @property courseNatureCode 课程性质代码
 * @property courseCategoryCode 课程大类代码
 * @property qualityCourseCategory 素质课程类别
 * @property examMethodCode 考试方式代码
 * @property groupName 分组名称
 * @property courseSequence 课序号
 * @property adjustmentCategory 调整类别
 * @property genderRequirementCode 性别要求代码
 * @property genderRequirement 性别要求
 * @property gradeRestrictions 年级限制
 * @property majorRestrictions 专业限制
 * @property collegeRestrictions 院系限制
 * @property campusRestrictions 校区限制
 * @property classRestrictions 行政班限制
 * @property tagRestrictions 标签限制
 */
@Serializable
data class SelectableCourse(
    @SerialName("jx0404id")
    val id: String,
    @SerialName("kch")
    val courseId: String,
    @SerialName("kcmc")
    val name: String,
    @SerialName("xf")
    val credits: Double,
    @SerialName("zxs")
    val totalHours: Int,
    @SerialName("skls")
    val teacher: String,
    @SerialName("jg0101id")
    val teacherIds: String,
    @SerialName("sksj")
    val schedule: String,
    @SerialName("skdd")
    val classroom: String,
    @SerialName("xqid")
    val campusId: String,
    @SerialName("kkapList")
    val schedules: List<ClassSchedule> = emptyList(),
    @SerialName("xxrs")
    val capacity: Int,
    @SerialName("syrs")
    val remaining: Int,
    @SerialName("xkrs")
    val enrolled: Int,
    @SerialName("czrs")
    val preselected: Int,
    @SerialName("pkrs")
    val scheduledCapacity: Int,
    @SerialName("szkcflmc")
    val selectionCategory: String?,
    @SerialName("szkcfl")
    val selectionCategoryCode: String?,
    @SerialName("kcsx")
    val attributeCode: String,
    @SerialName("dwmc")
    val department: String,
    @SerialName("kkdw")
    val offeringUnitCode: String,
    @SerialName("ctsm")
    val conflictDescription: String,
    @SerialName("bj")
    val note: String? = null,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("sfkfxk")
    val isOpenForSelection: Boolean,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("sfYx")
    val isSelected: Boolean,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("isdel")
    val isDeleted: Boolean,
    @SerialName("xnxq01id")
    val semesterId: String,
    @SerialName("jx02id")
    val courseMasterId: String,
    @SerialName("xxcode")
    val offeringCode: String?,
    @SerialName("kcxzm")
    val courseNatureCode: String,
    @SerialName("kcdldm")
    val courseCategoryCode: String? = null,
    @SerialName("szkclb")
    val qualityCourseCategory: String? = null,
    @SerialName("ksfs")
    val examMethodCode: String,
    @SerialName("fzmc")
    val groupName: String? = null,
    @SerialName("kxh")
    val courseSequence: String? = null,
    @SerialName("tzdlb")
    val adjustmentCategory: String,
    @SerialName("xbyq")
    val genderRequirementCode: String? = null,
    @SerialName("xbyqmc")
    val genderRequirement: String? = null,
    @SerialName("njxzList")
    val gradeRestrictions: List<List<String>> = emptyList(),
    @SerialName("zyxzList")
    val majorRestrictions: List<List<String>> = emptyList(),
    @SerialName("yxxzList")
    val collegeRestrictions: List<List<String>> = emptyList(),
    @SerialName("xqxzList")
    val campusRestrictions: List<List<String>> = emptyList(),
    @SerialName("xzbxzList")
    val classRestrictions: List<List<String>> = emptyList(),
    @SerialName("bqxzList")
    val tagRestrictions: List<List<String>> = emptyList(),
) {
    /** 是否已满 */
    val isFull: Boolean
        get() = remaining <= 0

    /** 选课情况，如 "45 / 45" */
    val enrollmentStatus: String
        get() = "$remaining / $capacity"

    /**
     * 结构化排课明细。
     *
     * @property weeks 上课周次列表
     * @property weekRange 周次范围，如 "1-16"
     * @property dayOfWeek 星期，1=周一
     * @property sections 节次，如 "3-4"
     * @property classroom 教室
     * @property timeDetail 课程时间详情
     */
    @Serializable
    data class ClassSchedule(
        @SerialName("skzcList")
        val weeks: List<String>,
        @SerialName("kkzc")
        val weekRange: String,
        @SerialName("xq")
        val dayOfWeek: Int,
        @SerialName("skjcmc")
        val sections: String,
        @SerialName("jsmc")
        val classroom: String,
        @SerialName("kcsj")
        val timeDetail: String? = null,
    )
}