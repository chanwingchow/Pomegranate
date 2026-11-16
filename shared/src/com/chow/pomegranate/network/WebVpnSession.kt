package com.chow.pomegranate.network

import com.chow.pomegranate.service.academic.affairs.internal.decodeCookies
import com.chow.pomegranate.service.academic.affairs.internal.encodeCookies
import com.chow.pomegranate.session.CookieStorage
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.time.Clock

enum class WebVpnStage { Disconnected, Restoring, Connecting, Otp, Captcha, Connected, Expired, Failed }
data class WebVpnState(val policy: ConnectionPolicy = ConnectionPolicy.Automatic, val stage: WebVpnStage = WebVpnStage.Disconnected,
    val account: String = "", val viaProxy: Boolean = false, val message: String? = null, val captcha: ByteArray? = null, val revision: Long = 0)

/** 门户认证独立于教务登录，不保存门户密码或 OTP；仅加密保存已验证的网关 Cookie。 */
class WebVpnSession(private val persistence: WebVpnPersistence, val router: WebVpnUrlRouter = WebVpnUrlRouter(), engine: HttpClientEngine? = null) {
    val cookies = CookieStorage()
    val state: StateFlow<WebVpnState>
        field = MutableStateFlow(WebVpnState())
    private val mutex = Mutex()
    private var loaded = false
    private var businessAccount: String? = null
    private var pendingForm: Pair<Url, Map<String, String>>? = null
    private val json = Json { ignoreUnknownKeys = true }
    val identity: String get() = state.value.let { "${it.revision}:${it.viaProxy}:${it.account}:${businessAccount.orEmpty()}" }
    private val client: HttpClient
    init {
        val configure: HttpClientConfig<*>.() -> Unit = {
            followRedirects = false
            install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 8_000; socketTimeoutMillis = 20_000 }
            install(HttpCookies) { storage = cookies }
            defaultRequest { header(HttpHeaders.AcceptEncoding, "identity") }
        }
        client = if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
    }

    private suspend fun load() {
        if (loaded) return
        val saved = persistence.load()
        cookies.restore(decodeCookies(saved.cookies.orEmpty()))
        state.value = WebVpnState(saved.policy, if (saved.account == null) WebVpnStage.Disconnected else WebVpnStage.Restoring,
            saved.account.orEmpty(), viaProxy = saved.policy == ConnectionPolicy.Proxy, revision = state.value.revision + 1)
        loaded = true
    }
    suspend fun initialize() = mutex.withLock { load() }

    suspend fun setPolicy(policy: ConnectionPolicy) = mutex.withLock {
        load(); persistence.policy(policy)
        cancelChallengeInternal()
        state.value = state.value.copy(policy = policy, viaProxy = policy == ConnectionPolicy.Proxy, message = null, revision = state.value.revision + 1)
    }

    suspend fun connection(): ServiceConnection = mutex.withLock {
        load()
        if (state.value.policy == ConnectionPolicy.Direct || state.value.policy == ConnectionPolicy.Automatic && !state.value.viaProxy)
            return@withLock ServiceConnection(ConnectionMode.Direct, identity)
        ensureGateway()
        checkAccount()
        ServiceConnection(ConnectionMode.WebVpn, identity)
    }
    suspend fun fallback(): ServiceConnection? = mutex.withLock {
        load()
        if (state.value.policy != ConnectionPolicy.Automatic) return@withLock null
        if (!state.value.viaProxy) state.value = state.value.copy(viaProxy = true, revision = state.value.revision + 1)
        ensureGateway()
        checkAccount()
        ServiceConnection(ConnectionMode.WebVpn, identity)
    }
    suspend fun requireAccount(account: String) = mutex.withLock {
        load()
        if (state.value.policy == ConnectionPolicy.Proxy || state.value.viaProxy) {
            ensureGateway()
            checkAccount(account)
        }
        businessAccount = account
    }
    private fun checkAccount(account: String? = businessAccount) { check(account == null || state.value.account == account) { "WebVPN 与登录账号不同，请先连接对应账号的 WebVPN。" } }
    fun releaseAccount() { businessAccount = null }
    private suspend fun ensureGateway() {
        if (state.value.stage == WebVpnStage.Restoring) {
            if (gatewayActive()) state.value = state.value.copy(stage = WebVpnStage.Connected)
            else expireInternal()
        }
        if (state.value.stage != WebVpnStage.Connected) {
            state.value = state.value.copy(message = "直连不可用或已选择代理，请先连接 WebVPN。")
            throw WebVpnLoginRequiredException()
        }
    }
    private suspend fun gatewayActive(): Boolean = try {
        val result = client.get("${router.origin}/rump_frontend/getHomeParam/").bodyAsText()
        json.parseToJsonElement(result).jsonObject["code"]?.jsonPrimitive?.content == "0"
    } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { false }

    suspend fun begin(account: String, password: String) = mutex.withLock {
        load()
        require(account.isNotBlank() && password.isNotBlank()) { "请输入学号和统一身份认证密码。" }
        persistence.forget(state.value.account.takeIf(String::isNotBlank)); cookies.clear()
        businessAccount = null; pendingForm = null
        state.value = state.value.copy(account = account.trim(), stage = WebVpnStage.Connecting, message = null, captcha = null,
            revision = state.value.revision + 1)
        try {
            val response = follow(client.get("${router.origin}/rump_frontend/login/"))
            val form = Ksoup.parse(response.bodyAsText()).getElementById("pwdFromId") ?: error("认证页面格式已变化。")
            val action = router.redirect(response.request.url, form.attr("action"), ConnectionMode.WebVpn)
            val fields = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }.toMutableMap()
            val salt = form.getElementById("pwdEncryptSalt")?.attr("value") ?: error("缺少认证参数。")
            val execution = form.getElementById("execution")?.attr("value") ?: error("缺少认证参数。")
            fields.putAll(mapOf("username" to state.value.account, "password" to CasPasswordCipher.encrypt(password, salt),
                "execution" to execution, "_eventId" to "submit", "cllt" to "userNameLogin", "dllt" to "generalLogin", "captcha" to ""))
            pendingForm = action to fields
            val captchaCheck = requestUrl("/authserver/checkNeedCaptcha.htl").let { url -> client.get(url) { parameter("username", state.value.account) }.bodyAsText() }
            val needsCaptcha = runCatching { json.parseToJsonElement(captchaCheck).jsonObject["isNeed"]?.jsonPrimitive?.booleanOrNull == true }.getOrDefault(false)
            if (needsCaptcha) {
                val image = client.get(requestUrl("/authserver/getCaptcha.htl")) { parameter("t", Clock.System.now().toEpochMilliseconds()) }.readRawBytes()
                state.value = state.value.copy(stage = WebVpnStage.Captcha, captcha = image)
            } else submitPassword("")
        } catch (cancelled: CancellationException) { abandonCancelled(); throw cancelled }
        catch (_: Exception) { fail("WebVPN 认证未完成，请检查网络和统一身份认证密码后重试。") }
    }

    suspend fun submitCaptcha(code: String) = mutex.withLock {
        check(state.value.stage == WebVpnStage.Captcha) { "验证码已失效，请重新连接。" }
        try { state.value = state.value.copy(message = null); submitPassword(code.trim()) }
        catch (cancelled: CancellationException) { abandonCancelled(); throw cancelled }
        catch (_: Exception) { fail("认证失败，请重新连接并检查验证码。") }
    }
    private suspend fun submitPassword(captcha: String) {
        val (action, fields) = requireNotNull(pendingForm)
        val post = URLBuilder(action).apply { parameters["service"] = router.callback }.build()
        val response = follow(client.submitForm(post.toString(), Parameters.build { (fields + ("captcha" to captcha)).forEach { (k, v) -> append(k, v) } }))
        pendingForm = null
        if (gatewayActive()) { accept(); return }
        val html = response.bodyAsText()
        if (response.request.url.encodedPath.contains("reAuth") || html.contains("otpCode") || html.contains("reAuthSubmit"))
            state.value = state.value.copy(stage = WebVpnStage.Otp, captcha = null, message = null)
        else {
            val message = Ksoup.parse(html).selectFirst("#showErrorTip, #msg, #errorMsg, .auth_error")?.text()?.take(180)?.takeIf(String::isNotBlank)
            fail(message ?: "统一认证未完成，可能需要其他验证方式，请稍后重试。")
        }
    }
    suspend fun submitOtp(code: String) = mutex.withLock {
        check(state.value.stage == WebVpnStage.Otp) { "验证已失效，请重新连接。" }
        if (!code.matches(Regex("\\d{6}"))) { state.value = state.value.copy(message = "请输入六位动态验证码。"); return@withLock }
        state.value = state.value.copy(message = null)
        try {
            val response = client.submitForm(requestUrl("/authserver/reAuthCheck/reAuthSubmit.do").toString(), parameters {
                append("service", router.callback); append("reAuthType", "10"); append("isMultifactor", "true"); append("otpCode", code)
                listOf("password", "dynamicCode", "uuid", "answer1", "answer2").forEach { append(it, "") }
            })
            val result = json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (result["code"]?.jsonPrimitive?.content != "reAuth_success") {
                state.value = state.value.copy(stage = WebVpnStage.Otp, message = "动态验证码验证失败，请使用最新验证码重试。")
                return@withLock
            }
            follow(client.get(requestUrl("/authserver/login")) { parameter("service", router.callback) })
            check(gatewayActive()) { "网关会话未建立。" }
            accept()
        } catch (cancelled: CancellationException) { abandonCancelled(); throw cancelled }
        catch (_: Exception) { state.value = state.value.copy(stage = WebVpnStage.Otp, message = "连接或验证未完成，请重试；过期后请重新连接。") }
    }
    private suspend fun requestUrl(path: String) = router.route(Url("http://authserver.gdufe.edu.cn$path"), ConnectionMode.WebVpn)
    private suspend fun accept() {
        state.value = state.value.copy(stage = WebVpnStage.Connected, message = null, captcha = null,
            viaProxy = state.value.policy == ConnectionPolicy.Proxy || state.value.policy == ConnectionPolicy.Automatic && state.value.viaProxy,
            revision = state.value.revision + 1)
        persistence.save(state.value.account, encodeCookies(cookies.snapshot()))
    }
    suspend fun saveCookies() = mutex.withLock {
        if (state.value.stage == WebVpnStage.Connected) persistence.save(state.value.account, encodeCookies(cookies.snapshot()))
    }
    suspend fun expired() = mutex.withLock { expireInternal() }
    private suspend fun expireInternal() {
        persistence.forget(state.value.account.takeIf(String::isNotBlank))
        cookies.clear(); pendingForm = null
        state.value = state.value.copy(stage = WebVpnStage.Expired, captcha = null, message = "WebVPN 已过期，请重新连接。", revision = state.value.revision + 1)
    }
    suspend fun disconnect() = mutex.withLock {
        load(); expireInternal(); state.value = state.value.copy(stage = WebVpnStage.Disconnected, message = null,
            account = "", viaProxy = state.value.policy == ConnectionPolicy.Proxy)
    }
    suspend fun forgetAccount(account: String) = mutex.withLock {
        load(); persistence.forget(account)
        if (state.value.account == account) { cookies.clear(); state.value = state.value.copy(stage = WebVpnStage.Disconnected, account = "", revision = state.value.revision + 1) }
    }
    suspend fun cancelChallenge() = mutex.withLock {
        cancelChallengeInternal()
    }
    private suspend fun cancelChallengeInternal() {
        if (state.value.stage in setOf(WebVpnStage.Otp, WebVpnStage.Captcha, WebVpnStage.Connecting)) {
            cookies.clear(); pendingForm = null
            state.value = state.value.copy(stage = WebVpnStage.Disconnected, message = null, captcha = null, account = "", revision = state.value.revision + 1)
        }
    }
    private fun fail(message: String) { pendingForm = null; state.value = state.value.copy(stage = WebVpnStage.Failed, message = message, captcha = null) }
    private suspend fun abandonCancelled() = withContext(NonCancellable) {
        cookies.clear(); pendingForm = null
        state.value = state.value.copy(stage = WebVpnStage.Disconnected, account = "", captcha = null, revision = state.value.revision + 1)
    }

    /** 显式处理 POST 302/303 和已确认的入口跳转；不执行网页脚本。 */
    private suspend fun follow(initial: HttpResponse): HttpResponse {
        var response = initial
        repeat(10) {
            val location = response.headers[HttpHeaders.Location]
            val httpRedirect = response.status.value in setOf(301, 302, 303, 307, 308)
            val htmlRedirect = if (!httpRedirect && response.contentType()?.match(ContentType.Text.Html) == true) {
                val doc = Ksoup.parse(response.bodyAsText())
                doc.selectFirst("a#url[href]")?.attr("href") ?: doc.selectFirst("meta[http-equiv=refresh]")?.attr("content")
                    ?.substringAfter("url=", "")?.trim()?.trim('\'', '"')?.takeIf(String::isNotEmpty)
            } else null
            val next = if (httpRedirect) location else htmlRedirect
            if (next.isNullOrBlank()) return response
            val url = router.redirect(response.request.url, next, ConnectionMode.WebVpn)
            check(router.isGateway(url)) { "认证跳转离开 WebVPN 网关。" }
            val keepBody = httpRedirect && response.status.value in setOf(307, 308)
            response = client.request(url) {
                method = if (keepBody) response.request.method else HttpMethod.Get
                if (keepBody) setBody(response.request.content) else setBody(EmptyContent)
            }
        }
        error("认证跳转次数过多。")
    }
    fun close() { client.close() }
}
