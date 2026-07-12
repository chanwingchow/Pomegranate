package com.chow.pomegranate.service.extracurricular.model

import com.chow.pomegranate.service.shared.IntBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 事件。
 *
 * @property id 唯一标识
 * @property name 活动名称
 * @property description 描述
 * @property category 分类
 * @property score 分数
 * @property location 地点
 * @property coverUrl 封面
 * @property organizer 主办方
 * @property coOrganizer 协办方
 * @property admin 管理员
 * @property phoneNumber 手机号
 * @property teacher 指导老师
 * @property eventType 类型名称
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property enrollEndTime 报名截止时间
 * @property trainingHours 培训学时
 * @property needSubmit 是否需要提交作业
 * @property peopleLimit 人数上限
 * @property participantCount 当前参与人数
 * @property note 备注
 * @property isFinished 是否完结
 * @property applyStatus 报名状态
 * @property creditStatus 学分状态
 * @property statusAll 总体状态
 * @property attachments 附件
 */
@Serializable
data class Event(
    @SerialName("actId") val id: Int,
    val name: String,
    @SerialName("introduce") val description: String,
    @SerialName("className") val category: String,
    @SerialName("hours") val score: Double,
    @SerialName("pitchAddress") val location: String,
    @SerialName("haibaoUrl") val coverUrl: String,
    @SerialName("zhubanName") val organizer: String,
    @SerialName("xiebanName") val coOrganizer: String? = null,
    @SerialName("adminName") val admin: String,
    @SerialName("adminContact") val phoneNumber: String,
    @SerialName("teacherName") val teacher: String? = null,
    @SerialName("typeName") val eventType: String,
    val startTime: String,
    val endTime: String,
    val enrollEndTime: String,
    @SerialName("classHours") val trainingHours: Double,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("subJob")
    val needSubmit: Boolean,
    @SerialName("peopleLimit") val peopleLimit: Int,
    @SerialName("peopleCount") val participantCount: Int,
    @SerialName("remark") val note: String? = null,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("finish")
    val isFinished: Boolean,
    @SerialName("apply_status")
    val applyStatus: Int,
    @SerialName("creditStatus")
    val creditStatus: Int,
    @SerialName("statusAll") val statusAll: Int,
    @SerialName("fuJianList") val attachments: List<String>? = null,
)