package com.chow.pomegranate.service.academic.affairs.model

/**
 * 选课学分总览。
 *
 * @property note 选课备注
 * @property progress 选课学分进度
 */
data class CreditSummary(
    val note: String,
    val progress: List<CreditProgress>,
) {
    /**
     * 选课学分进度。
     *
     * @property category 类别
     * @property selected 已选学分
     * @property limit 学分上限
     */
    data class CreditProgress(
        val category: String,
        val selected: String,
        val limit: String,
    )
}