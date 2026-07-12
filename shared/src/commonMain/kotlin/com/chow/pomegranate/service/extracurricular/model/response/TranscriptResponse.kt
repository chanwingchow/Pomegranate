package com.chow.pomegranate.service.extracurricular.model.response

import com.chow.pomegranate.service.extracurricular.model.ExtracurricularGrade
import kotlinx.serialization.Serializable

/**
 * 成绩响应。
 */
@Serializable
internal data class TranscriptResponse(
    val code: Int,
    val msg: String,
    val data: List<ExtracurricularGrade>,
)