package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.LoginErrorMessageParser
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.parameters
import kotlin.time.Clock

/**
 * 教务系统认证模块实现。
 */
internal class AuthImpl(
    private val httpClient: HttpClient,
) : AcademicAffairs.Auth {
    override suspend fun getCaptcha(): ByteArray {
        val bytes = httpClient.get("/jsxsd/verifycode.servlet")
            .readRawBytes()

        return bytes
    }

    override suspend fun login(param: LoginParam): LoginResult {
        // 验证码错误，服务器会返回这个信息
        val captchaErrorMsg = "验证码错误!!"

        return try {
            require(param.captcha.length == 4) { captchaErrorMsg }

            // 提交表单
            val text = httpClient.submitForm(
                "jsxsd/xk/LoginToXkLdap",
                parameters {
                    append("USERNAME", param.username)
                    append("PASSWORD", param.password)
                    append("RANDOMCODE", param.captcha)
                },
            ).bodyAsText()

            if (text.isEmpty()) {
                // 登录成功
                LoginResult.Success
            } else {
                // 解析 HTML
                val message = LoginErrorMessageParser.parse(text)
                error(message)
            }
        } catch (e: Exception) {
            if (e.message == captchaErrorMsg) {
                // 验证码错误
                LoginResult.Fail.CaptchaError
            } else {
                // 未知错误
                LoginResult.Fail.Unknow(e)
            }
        }
    }

    override suspend fun logout() {
        httpClient.get("/jsxsd/xk/LoginToXk") {
            parameter("method", "exit")
            parameter("tktime", Clock.System.now().toEpochMilliseconds())
        }
    }
}