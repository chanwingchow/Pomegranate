package com.chow.pomegranate.network

import com.chow.pomegranate.session.CookieStorage
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.AttributeKey
import kotlinx.io.IOException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.*

internal val OriginalServiceUrl = AttributeKey<Url>("PomegranateOriginalServiceUrl")
private val RequestConnectionMode = AttributeKey<ConnectionMode>("PomegranateConnectionMode")
private val ConnectionIdentity = AttributeKey<String>("PomegranateConnectionIdentity")
data class ServiceConnection(val mode: ConnectionMode, val identity: String = "")
internal fun HttpRequestBuilder.serviceUrl(): Url = attributes.getOrNull(OriginalServiceUrl) ?: url.build()
internal fun isCourseMutation(url: Url) = url.encodedPath.endsWith("xkOper") || url.encodedPath.endsWith("xstkOper")
private val courseSearchPaths = CourseSelectCategory.entries.map { "/jsxsd/xsxkkc/xsxk${it.searchRoute}xk" }.toSet()

class WebVpnLoginRequiredException : IllegalStateException("WebVPN 未连接或已过期，请重新连接后再登录。")
class ConnectionChangedException(cause: Throwable) : IllegalStateException("直连不可用，已切换到 WebVPN。为避免重复提交，请重新操作。", cause)

/** 默认地址解析后保留业务地址，实际发送前再选择连接。 */
private object RouteRequest : ClientHook<suspend (HttpRequestBuilder) -> Unit> {
    override fun install(client: HttpClient, handler: suspend (HttpRequestBuilder) -> Unit) {
        client.requestPipeline.intercept(HttpRequestPipeline.Transform) { handler(context) }
    }
}

internal class ServiceCookieStorage(private val direct: CookieStorage, private val vpn: CookieStorage,
    private val router: WebVpnUrlRouter) : CookiesStorage {
    private fun storage(url: Url) = if (router.isGateway(url)) vpn else direct
    override suspend fun get(requestUrl: Url) = storage(requestUrl).get(requestUrl)
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = storage(requestUrl).addCookie(requestUrl, cookie)
    override fun close() = Unit
}

internal fun HttpClientConfig<*>.installServiceTransport(
    router: WebVpnUrlRouter,
    connection: suspend () -> ServiceConnection,
    currentIdentity: () -> String = { "" },
    onDirectUnavailable: suspend () -> ServiceConnection? = { null },
    onExpired: suspend () -> Unit = {},
    afterResponse: suspend () -> Unit = {},
) {
    followRedirects = false
    install(createClientPlugin("PomegranateServiceTransport") {
        on(RouteRequest) { request ->
            request.attributes.put(OriginalServiceUrl, router.original(request.url.build()))
        }
        // HttpCookies 已先安装：匹配 Cookie 后再检查账号/连接版本，避免跨账号发送。
        on(SendingRequest) { request, _ ->
            check(request.attributes.getOrNull(ConnectionIdentity).orEmpty() == currentIdentity()) { "连接方式或账号已变化，请重新操作。" }
        }
        on(Send) { request ->
            // 会话插件先完成等待/自动登录，再绑定账号版本；等待前取版本会误判合法登录。
            val selection = connection()
            val logical = request.serviceUrl()
            request.attributes.put(RequestConnectionMode, selection.mode)
            request.attributes.put(ConnectionIdentity, selection.identity)
            request.url.takeFrom(router.route(logical, selection.mode))
            // 每次发送都有独立的超时上下文，同时跟随调用者取消；不取消原始请求。
            fun attempt(source: HttpRequestBuilder = request): HttpRequestBuilder = HttpRequestBuilder().takeFrom(source).also { child ->
                request.executionContext.invokeOnCompletion { cause ->
                    val job = child.executionContext as CompletableJob
                    if (cause == null) job.complete() else job.completeExceptionally(cause)
                }
            }
            var mode = selection.mode
            var next = attempt()
            var call = try { proceed(next) }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val timeout = generateSequence<Throwable>(error) { it.cause }.take(12).any { it is HttpRequestTimeoutException }
                if (error is CancellationException && !timeout) throw error
                val retryable = error is IOException || timeout
                val readOnly = request.method in setOf(HttpMethod.Get, HttpMethod.Head) && !isCourseMutation(logical) ||
                    request.method == HttpMethod.Post && logical.encodedPath in courseSearchPaths
                val certificateFailure = generateSequence<Throwable>(error) { it.cause }.take(12).any {
                    it::class.simpleName.orEmpty().let { name -> name.contains("SSL") || name.contains("Certificate") }
                }
                if (mode != ConnectionMode.Direct || !retryable || certificateFailure || !router.isTarget(logical)) throw error
                val fallback = onDirectUnavailable() ?: throw error
                if (!readOnly) throw ConnectionChangedException(error)
                mode = fallback.mode
                // 超时会取消旧 builder 的 executionContext，重试必须使用新的请求上下文。
                next = attempt().apply {
                    attributes.put(RequestConnectionMode, mode)
                    attributes.put(ConnectionIdentity, fallback.identity)
                    url.takeFrom(router.route(logical, mode))
                    headers.remove(HttpHeaders.Cookie)
                }
                proceed(next)
            }
            var hops = 0
            while (true) {
                val location = call.response.headers[HttpHeaders.Location]
                val target = location?.let { router.redirect(call.request.url, it, mode) }
                val protectedRequest = logical.host in setOf("jwxt.gdufe.edu.cn", "2ketang.gdufe.edu.cn")
                val loginTarget = target?.let { router.original(it) }?.let {
                    it.host == "authserver.gdufe.edu.cn" || router.isGateway(it) && it.encodedPath.contains("/rump_frontend/login")
                } == true
                val loginHtml = if (protectedRequest && mode == ConnectionMode.WebVpn &&
                    call.response.contentType()?.match(ContentType.Text.Html) == true)
                    call.response.bodyAsText().take(16_000).let { it.contains("pwdFromId") || it.contains("reAuthCheck/reAuth") } else false
                if (protectedRequest && mode == ConnectionMode.WebVpn && (loginTarget || loginHtml)) {
                    onExpired()
                    throw WebVpnLoginRequiredException()
                }
                if (call.response.status.value !in setOf(301, 302, 303, 307, 308) || target == null ||
                    request.method !in setOf(HttpMethod.Get, HttpMethod.Head) || isCourseMutation(logical)) break
                check(++hops <= 8) { "请求重定向次数过多，请重新连接。" }
                if (call.request.url.protocol == URLProtocol.HTTPS && target.protocol != URLProtocol.HTTPS) break
                val previousOrigin = router.original(call.request.url)
                val nextOrigin = router.original(target)
                next = attempt(next).apply {
                    url.takeFrom(target)
                    headers.remove(HttpHeaders.Cookie)
                    if (previousOrigin.host != nextOrigin.host || previousOrigin.port != nextOrigin.port || previousOrigin.protocol != nextOrigin.protocol) {
                        headers.remove(HttpHeaders.Authorization)
                        headers.remove("X-token")
                    }
                }
                call = proceed(next)
            }
            afterResponse()
            call
        }
    })
}
