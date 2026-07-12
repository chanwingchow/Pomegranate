package com.chow.pomegranate.service.extracurricular.internal

import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.response.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/**
 * 第二课堂认证模块实现。
 */
internal class AuthImpl(
    private val httpClient: HttpClient,
    private val userId: MutableStateFlow<String?>,
    private val sessionId: MutableStateFlow<String?>,
) : ExtracurricularActivity.Auth {
    override suspend fun login(userId: String, password: String) {
        val response = httpClient.submitForm(
            "/apps/common/login",
            parameters {
                append(
                    "para",
                    Json.encodeToString(
                        mapOf(
                            "account" to userId,
                            "password" to password,
                            "school" to "10018",
                        ),
                    ),
                )
            },
        )
        val body = response.body<LoginResponse>()

        check(body.code == 200) { body.msg }

        this.userId.value = userId
        sessionId.value = body.data?.id
    }
}