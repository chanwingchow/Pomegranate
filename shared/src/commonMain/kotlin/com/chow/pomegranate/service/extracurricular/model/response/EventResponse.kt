package com.chow.pomegranate.service.extracurricular.model.response

import com.chow.pomegranate.service.extracurricular.model.Event
import kotlinx.serialization.Serializable

/**
 * 事件详情响应。
 */
@Serializable
internal data class EventResponse(
    override val code: Int,
    override val msg: String,
    override val data: Event,
) : ExtracurricularResponse<Event>