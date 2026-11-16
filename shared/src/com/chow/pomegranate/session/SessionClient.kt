package com.chow.pomegranate.session

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import com.chow.pomegranate.network.*

/** 教务系统地址。 */
private const val ACADEMIC_AFFAIRS_URL = "http://jwxt.gdufe.edu.cn"

/**
 * 教务系统 Http 客户端的公共配置。
 *
 * @param cookieStorage Cookie 仓库，由会话层统一持久化
 */
private fun HttpClientConfig<*>.configureAcademicAffairs(cookieStorage: CookiesStorage, webVpn: WebVpnSession? = null) {
    // 默认请求
    defaultRequest {
        url(ACADEMIC_AFFAIRS_URL)
    }
    // Cookie
    install(HttpCookies) {
        storage = if (webVpn != null && cookieStorage is CookieStorage) ServiceCookieStorage(cookieStorage, webVpn.cookies, webVpn.router) else cookieStorage
    }
    if (webVpn != null) installServiceTransport(webVpn.router, webVpn::connection, { webVpn.identity }, webVpn::fallback, webVpn::expired, webVpn::saveCookies)
    // 超时：Ktor 默认不设超时，慢响应或连接挂死时界面会一直转圈。
    // 教务是校园网内网服务，整体 20 秒足够；个别大接口按需单独放宽。
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 20_000
    }
    install(ContentNegotiation) {
        // Json 解析
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        json(json)
        json(
            json,
            // 教务系统返回的数据是 Json，但标记的类型是 HTML
            contentType = ContentType.Text.Html,
        )
    }
}

/**
 * 返回教务系统的基础 Http 客户端。
 *
 * 不包含会话校验，用于登录、登出与会话校验本身，避免递归重新登录。
 *
 * @param cookieStorage Cookie 仓库
 */
fun baseAcademicAffairsHttpClient(cookieStorage: CookiesStorage, webVpn: WebVpnSession? = null): HttpClient = HttpClient {
    configureAcademicAffairs(cookieStorage, webVpn)
}

/**
 * 返回教务系统的会话 Http 客户端。
 *
 * 在基础客户端之上安装会话校验：请求被重定向到登录页时自动重新登录并重放请求。
 *
 * @param cookieStorage Cookie 仓库
 * @param sessionManager 会话管理器，延迟获取以避免与客户端相互构造
 */
fun sessionAcademicAffairsHttpClient(
    cookieStorage: CookiesStorage,
    sessionManager: Lazy<SessionManager>,
    webVpn: WebVpnSession? = null,
): HttpClient = HttpClient {
    // 先等待登录，再由网络层选择连接并绑定当前账号版本。
    installSessionCheck(sessionManager)
    configureAcademicAffairs(cookieStorage, webVpn)

    // 会话校验内部会重新登录，留给它的时间必须覆盖一次完整登录（含验证码识别）
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }
}

/**
 * 安装会话校验插件。
 *
 * 未登录时不发起请求：先等待自动登录，等不到就抛出 [SessionExpiredException]。
 * 这样任何新增的受保护接口都不会在未登录状态下发出无效请求。
 *
 * @param sessionManager 会话管理器
 */
private fun HttpClientConfig<*>.installSessionCheck(sessionManager: Lazy<SessionManager>) {
    val sessionPlugin = createClientPlugin("PomegranateSession") {
        on(Send) { request ->
            // 未登录时先等待自动登录，避免发出必然无效的请求
            val loggedIn = withTimeoutOrNull(LOGIN_WAIT_TIMEOUT_MILLIS) {
                sessionManager.value.awaitLogin()
            } != null

            if (!loggedIn) throw SessionExpiredException()

            var call = proceed(request)

            if (!call.response.isLoginPage()) return@on call

            // 选退课即便使用 GET 也有副作用，不因登录态/连接变化自动重放。
            if (isCourseMutation(request.serviceUrl())) throw SessionExpiredException()

            // 登录已过期，重新登录后重放一次
            runCatching { sessionManager.value.refresh() }
                .onFailure { throw SessionExpiredException() }

            call = proceed(request)

            if (call.response.isLoginPage()) throw SessionExpiredException()

            call
        }
    }

    install(sessionPlugin)
}

/** 等待自动登录的最长时间。 */
private const val LOGIN_WAIT_TIMEOUT_MILLIS = 15_000L
