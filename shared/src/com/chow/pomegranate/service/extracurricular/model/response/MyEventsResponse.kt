package com.chow.pomegranate.service.extracurricular.model.response

import com.chow.pomegranate.service.extracurricular.model.MyEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 我的事件响应。
 */
@Serializable
internal data class MyEventsResponse(
    override val code: Int,
    override val msg: String,
    override val data: MyEventsData,
) : ExtracurricularResponse<MyEventsData>


/**
 * 我的事件数据。
 */
@Serializable
internal data class MyEventsData(
    @SerialName("records")
    val items: List<MyEvent>,
)