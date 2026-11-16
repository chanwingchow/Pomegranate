package com.chow.pomegranate.service.extracurricular.internal

import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.Transcript
import com.chow.pomegranate.service.extracurricular.model.TranscriptLogs
import com.chow.pomegranate.service.extracurricular.model.response.TranscriptLogsResponse
import com.chow.pomegranate.service.extracurricular.model.response.TranscriptResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * 第二课堂成就实现。
 */
internal class AchievementImpl(
    private val httpClient: HttpClient,
    private val userId: StateFlow<String?>,
    private val sessionId: StateFlow<String?>,
) : ExtracurricularActivity.Achievement {
    override suspend fun getTranscript(): Transcript {
        val body = httpClient.get("apps/user/achievement/by-classify-list") {
            parameter("para", Json.encodeToString(mapOf("userId" to sessionId.value!!)))
        }.body<TranscriptResponse>()

        check(body.code == 200) { body.msg }

        return Transcript(
            userId = userId.value!!,
            items = body.data,
        )
    }

    override suspend fun getTranscriptLogs(): TranscriptLogs {
        val body = httpClient.get("apps/user/achievement/by-classify-list-detail")
            .body<TranscriptLogsResponse>()

        check(body.code == 200) { body.msg }

        return TranscriptLogs(
            userId = userId.value!!,
            items = body.data,
        )
    }
}