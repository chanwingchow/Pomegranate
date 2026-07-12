package com.chow.pomegranate.service.extracurricular.model.response

import com.chow.pomegranate.service.extracurricular.model.BasicEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 基础事件响应。
 */
@Serializable
internal data class BasicEventsResponse(
    override val code: Int,
    override val msg: String,
    override val data: BasicEventsData,
) : ExtracurricularResponse<BasicEventsData>

/**
 * 基础事件数据。
 */
@Serializable
internal data class BasicEventsData(
    @SerialName("records")
    val items: List<BasicEvent>,
)