package com.chow.pomegranate.service.extracurricular.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 成绩。
 *
 * @property userId 学号
 * @property items 成绩项
 */
@Serializable
data class Transcript(
    val userId: String,
    val items: List<ExtracurricularGrade>,
)

/**
 * 第二课堂成绩项。
 *
 * @property categoryId 分类标识
 * @property name 分类名称
 * @property score 已获得分数
 * @property requiredScore 学校要求最低分数
 * @property totalScore 总分额度
 * @property activityScore 活动分数
 * @property projectScore 项目分数
 * @property awardScore 奖励分数
 * @property averageScore 平均分
 * @property isRequired 是否必修
 * @property termMinScore 学期最低分数
 */
@Serializable
data class ExtracurricularGrade(
    @SerialName("id") val categoryId: Int,
    @SerialName("classifyName") val name: String,
    @SerialName("classifyHours") val score: Double,
    @SerialName("classifySchoolMinHours") val requiredScore: Double,
    @SerialName("classifySumHours") val totalScore: Double,
    @SerialName("classActHours") val activityScore: Double,
    @SerialName("classProHours") val projectScore: Double,
    @SerialName("classAwardHours") val awardScore: Double? = null,
    @SerialName("average") val averageScore: Double? = null,
    @SerialName("required") val isRequired: Int? = null,
    @SerialName("classifyTermMinHours") val termMinScore: Double? = null,
)