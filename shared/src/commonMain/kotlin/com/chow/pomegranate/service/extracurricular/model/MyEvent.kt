package com.chow.pomegranate.service.extracurricular.model

import com.chow.pomegranate.service.utils.IntBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 我的事件。
 * @property id 唯一标识
 * @property name 活动名称
 * @property category 分类
 * @property score 分数
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property organizer 主办方
 * @property imageUrl 封面
 * @property eventType 类型名称
 * @property isOnline 是否线上
 * @property enrollEndTime 报名截止时间
 * @property auditStatus 审核状态
 * @property auditNote 审核备注
 * @property role 角色
 * @property identity 身份标识
 * @property star 评分
 */
@Serializable
data class MyEvent(
    @SerialName("id") override val id: Int,
    @SerialName("name") override val name: String,
    @SerialName("category") override val category: String,
    @SerialName("hours") override val score: Double,
    @SerialName("startTime") override val startTime: Long,
    @SerialName("endTime") override val endTime: Long,
    @SerialName("orgName") override val organizer: String,
    @SerialName("logo") override val imageUrl: String?,
    @SerialName("typeName") override val eventType: String,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("oto")
    override val isOnline: Boolean,
    val enrollEndTime: Long,
    @SerialName("audit_status") val auditStatus: Int,
    @SerialName("audit_remark") val auditNote: String?,
    val role: Int? = null,
    val identity: Int? = null,
    val star: Int? = null,
) : IBasicEvent
