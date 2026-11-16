package com.chow.pomegranate.service.extracurricular.model.response

import com.chow.pomegranate.service.extracurricular.model.TranscriptLog
import kotlinx.serialization.Serializable

/**
 * 成绩日志响应。
 */
@Serializable
internal data class TranscriptLogsResponse(
    val code: Int,
    val msg: String,
    val data: List<TranscriptLog>,
)