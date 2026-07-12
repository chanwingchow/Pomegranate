package com.chow.pomegranate.service.extracurricular.model

import com.chow.pomegranate.service.utils.IntBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 事件。
 *
 * @property participantCount 参与人数
 */
@Serializable
data class BasicEvent(
    @SerialName("id") override val id: Int,
    @SerialName("name") override val name: String,
    @SerialName("category") override val category: String,
    @SerialName("hours") override val score: Double,
    @SerialName("startTime") override val startTime: Long,
    @SerialName("endTime") override val endTime: Long,
    @SerialName("orgName") override val organizer: String,
    @SerialName("logo") override val imageUrl: String?,
    @SerialName("typeName") override val eventType: String,
    @Serializable(IntBooleanSerializer::class)
    @SerialName("oto")
    override val isOnline: Boolean,
    @SerialName("canyuNum") val participantCount: Double,
): IBasicEvent
