package com.chow.pomegranate.service.extracurricular.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 成绩日志。
 *
 * @property userId 学号
 * @property items 日志项
 */
@Serializable
data class TranscriptLogs(
    val userId: String,
    val items: List<TranscriptLog>,
)

/**
 * 成绩日志项。
 *
 * @property activityName 活动名称
 * @property projectName 项目名称
 * @property optionName 选项名称
 * @property category 分类
 * @property score 分数
 * @property time 时间
 * @property semester 学期
 * @property sourceType 来源类型
 * @property note 备注
 */
@Serializable
data class TranscriptLog(
    @SerialName("actName") val activityName: String?,
    @SerialName("proName") val projectName: String?,
    @SerialName("optName") val optionName: String?,
    @SerialName("className") val category: String,
    val score: Double,
    @SerialName("sendTime") val time: Long,
    @SerialName("xueqiName") val semester: String,
    @SerialName("sourceType") val sourceType: Int? = null,
    val note: String? = null,
) {
    val displayName: String get() = activityName ?: projectName ?: optionName ?: ""
}