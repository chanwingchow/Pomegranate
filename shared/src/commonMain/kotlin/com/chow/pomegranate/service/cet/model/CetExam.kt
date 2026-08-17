package com.chow.pomegranate.service.cet.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.AmPmMarker

/**
 * CET 考试。
 *
 * @property name 考试名称
 * @property cet4Written 英语四级笔试
 * @property cet6Written 英语六级笔试
 * @property cet4Speaking 英语四级口语
 * @property cet6Speaking 英语六级口语
 * @property note 备注
 * @property pdfUrlString PDF URL
 */
data class CetExam(
    val name: String,
    val cet4Written: CetExamItem,
    val cet6Written: CetExamItem,
    val cet4Speaking: CetExamItem,
    val cet6Speaking: CetExamItem,
    val note: String,
    val pdfUrlString: String,
)

/**
 * CET 考试项。
 */
data class CetExamItem(
    val date: LocalDate,
    val languages: String,
    val amPmMarker: AmPmMarker? = null,
)