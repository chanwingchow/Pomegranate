package com.chow.pomegranate.session

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

/**
 * 登录页的特征片段。
 *
 * 教务系统在登录失效时不会返回 401，而是把请求重定向到登录页并返回 200，
 * 因此只能在响应正文中识别这些特征。
 */
private val LOGIN_PAGE_MARKERS = listOf(
    // 登录表单提交地址
    "LoginToXk",
    // 验证码接口
    "verifycode.servlet",
)

/** 参与登录页判定的正文长度，登录页的特征集中在页面头部。 */
private const val LOGIN_PAGE_SCAN_LENGTH = 4_000

/**
 * 判断 [response] 是否被重定向到了登录页。
 *
 * 读取正文可能失败（例如连接中断），此时按"不是登录页"处理，交由上层报错。
 */
internal suspend fun HttpResponse.isLoginPage(): Boolean = runCatching {
    bodyAsText().take(LOGIN_PAGE_SCAN_LENGTH).let { head ->
        LOGIN_PAGE_MARKERS.any { head.contains(it) }
    }
}.getOrDefault(false)
