package com.chow.pomegranate.service.extracurricular.internal

import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.encodedPath
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 第二课堂实现。
 */
internal class ExtracurricularActivityImpl(
    httpClient: HttpClient,
) : ExtracurricularActivity {
    /** 学号 */
    private val userId = MutableStateFlow<String?>(null)

    /** 会话 id */
    private val sessionId = MutableStateFlow<String?>(null)

    /** 请求头 X-token */
    private var token: String? = null

    init {
        httpClient.plugin(HttpSend).intercept { builder ->
            if (builder.url.encodedPath == "/apps/common/login") {
                // 登录时从相应头取 X-token
                execute(builder).also { call ->
                    token = call.response.headers["X-token"]
                }
            } else {
                // 其他请求自动注入 X-token
                builder.header("X-token", token)
                execute(builder)
            }
        }
    }

    override val auth: ExtracurricularActivity.Auth = AuthImpl(
        httpClient,
        userId = userId,
        sessionId = sessionId,
    )

    override val achievement: ExtracurricularActivity.Achievement = AchievementImpl(
        httpClient,
        userId = userId,
        sessionId = sessionId,
    )

    override val activity: ExtracurricularActivity.Activity = ActivityImpl(
        httpClient,
    )
}