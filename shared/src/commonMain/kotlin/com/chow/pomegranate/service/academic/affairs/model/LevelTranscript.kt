package com.chow.pomegranate.service.academic.affairs.model

import kotlinx.datetime.LocalDate

/**
 * 等级考试成绩。
 *
 * @property userId 学号
 * @property items 等级考试成绩项
 */
data class LevelTranscript(
    val userId: String,
    val items: List<LevelGrade>,
)

/**
 * 等级考试成绩项。
 *
 * @property name 考试名称
 * @property score 成绩
 * @property date 考试时间
 */
data class LevelGrade(
    val name: String,
    val score: String,
    val date: LocalDate,
)