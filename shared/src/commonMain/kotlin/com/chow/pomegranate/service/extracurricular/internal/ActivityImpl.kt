package com.chow.pomegranate.service.extracurricular.internal

import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.BasicEvent
import com.chow.pomegranate.service.extracurricular.model.Event
import com.chow.pomegranate.service.extracurricular.model.EventState
import com.chow.pomegranate.service.extracurricular.model.MyEvent
import com.chow.pomegranate.service.extracurricular.model.response.BasicEventsResponse
import com.chow.pomegranate.service.extracurricular.model.response.EventResponse
import com.chow.pomegranate.service.extracurricular.model.response.MyEventsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json

/**
 * 第二课堂活动实现。
 */
internal class ActivityImpl(
    private val httpClient: HttpClient,
) : ExtracurricularActivity.Activity {
    override suspend fun getEvents(): List<BasicEvent> {
        val body = getEventsByState().body<BasicEventsResponse>()

        check(body.code == 200) { body.msg }

        return body.data.items
    }

    override suspend fun getMyEvents(state: EventState): List<MyEvent> {
        val body = getEventsByState(state).body<MyEventsResponse>()

        check(body.code == 200) { body.msg }

        return body.data.items
    }

    /**
     * 通过 [state] 获取事件列表。
     */
    private suspend fun getEventsByState(state: EventState? = null): HttpResponse {
        val urlString = if (state == null) {
            // 所有事件
            "apps/activityImpl/list/getActivityByUser"
        } else {
            // 我的事件
            "apps/activityImpl/getmyjoinactivitylist"
        }

        val sharedParam = mapOf(
            "cur" to 1,
            "size" to 20,
        )

        val param = if (state == null) {
            sharedParam
        } else {
            sharedParam + mapOf("type" to state.id)
        }

        val response = httpClient.get(urlString) {
            parameter("para", Json.encodeToString(param))
        }

        return response
    }

    override suspend fun getEvent(id: Int): Event {
        val body = httpClient.get("apps/activityImpl/detail") {
            parameter("para", Json.encodeToString(mapOf("activityId" to id)))
        }.body<EventResponse>()

        require(body.code == 200) { body.msg }
        return body.data
    }
}