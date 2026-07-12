package com.chow.pomegranate.service.extracurricular.api

import com.chow.pomegranate.service.extracurricular.internal.ExtracurricularActivityImpl
import com.chow.pomegranate.service.extracurricular.model.BasicEvent
import com.chow.pomegranate.service.extracurricular.model.Event
import com.chow.pomegranate.service.extracurricular.model.EventState
import com.chow.pomegranate.service.extracurricular.model.MyEvent
import com.chow.pomegranate.service.extracurricular.model.Transcript
import com.chow.pomegranate.service.extracurricular.model.TranscriptLogs
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 第二课堂。
 */
fun ExtracurricularActivity(): ExtracurricularActivity {
    return ExtracurricularActivityImpl(
        httpClient = HttpClient {
            // 默认请求
            defaultRequest {
                url("http://2ketang.gdufe.edu.cn")
            }
            install(ContentNegotiation) {
                // Json 解析
                val json = Json {
                    encodeDefaults = true
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json(json)
            }
        },
    )
}

/**
 * 第二课堂。
 */
interface ExtracurricularActivity {
    /** 认证模块 */
    val auth: Auth

    /** 成就模块 */
    val achievement: Achievement

    /** 活动模块 */
    val activity: Activity

    /**
     * 认证模块。
     */
    interface Auth {
        suspend fun login(userId: String, password: String)
    }

    /**
     * 成就模块。
     */
    interface Achievement {
        /**
         * 返回成绩。
         */
        suspend fun getTranscript(): Transcript

        /**
         * 返回成绩日志。
         */
        suspend fun getTranscriptLogs(): TranscriptLogs
    }

    /**
     * 活动模块。
     */
    interface Activity {
        /**
         * 返回活动。
         */
        suspend fun getEvents(): List<BasicEvent>

        /**
         * 返回我的活动。
         */
        suspend fun getMyEvents(state: EventState): List<MyEvent>

        /**
         * 返回活动详情。
         */
        suspend fun getEvent(id: Int): Event
    }
}