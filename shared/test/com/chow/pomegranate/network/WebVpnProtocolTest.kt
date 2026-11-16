package com.chow.pomegranate.network

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WebVpnProtocolTest {
    @Test fun automaticFallbackRejectsABusinessAccountDifferentFromTheGatewayAccount() = runTest {
        val fixture = GatewayFixture()
        val session = WebVpnSession(MemoryVpnPersistence(), fixture.router, fixture.engine())
        try {
            session.begin("demo-user", "fake-password")
            session.submitOtp("123456")
            session.requireAccount("other-user") // 直连不使用门户会话，可先正常选择其他账号。
            val failure = assertFailsWith<IllegalStateException> { session.fallback() }
            assertTrue(failure.message.orEmpty().contains("账号不同"))
            assertFailsWith<IllegalStateException> { session.connection() }
            session.requireAccount("demo-user")
            assertEquals(ConnectionMode.WebVpn, session.connection().mode)
        } finally { session.close() }
    }
    @Test fun cipherMatchesPythonCompatibleCbcVectorIncludingUtf8AndPadding() = runTest {
        // OpenSSL AES-128-CBC，固定的虚构密码/前缀/IV；绝不包含用户凭据。
        assertEquals("RuHmQqcli5IsBN/z4mo6CrT8bhzylJ7J/01nBxQkV9ZEdo4dJQv0cW7UZRKtDxLl9ErEig0WJdQ76ovWOdTZKCfBJpF1YoOeiT/EPL/29XU=",
            CasPasswordCipher.encrypt("测试Pass+123", "0123456789abcdef", "A".repeat(64), "abcdef0123456789"))
    }

    @Test fun handlesHtmlJumpPostRedirectOtpAndGatewayValidationInOneSession() = runTest {
        val persistence = MemoryVpnPersistence()
        val fixture = GatewayFixture()
        val session = WebVpnSession(persistence, fixture.router, fixture.engine())
        try {
            session.begin("demo-user", "not-a-real-password")
            assertEquals(WebVpnStage.Otp, session.state.value.stage)
            assertNull(persistence.account, "出现 preauth Cookie 不应视为登录成功")
            session.submitOtp("111111")
            assertEquals(WebVpnStage.Otp, session.state.value.stage)
            session.submitOtp("123456")
            assertEquals(WebVpnStage.Connected, session.state.value.stage)
            assertEquals("demo-user", persistence.account)
            assertTrue(fixture.formServices.all { it == fixture.router.callback })
            assertFalse(persistence.cookies.orEmpty().contains("not-a-real-password"))
            assertFalse(persistence.cookies.orEmpty().contains("123456"))
            assertEquals(ConnectionMode.Direct, session.connection().mode, "自动模式预先连接 WebVPN 后仍优先直连")
            session.setPolicy(ConnectionPolicy.Proxy)
            session.requireAccount("demo-user")
            assertFailsWith<IllegalStateException> { session.requireAccount("other-user") }
            assertEquals(ConnectionMode.WebVpn, session.connection().mode, "错误账号不应覆盖当前绑定")
            session.requireAccount("demo-user")
            session.setPolicy(ConnectionPolicy.Automatic)
            assertEquals(ConnectionMode.Direct, session.connection().mode)
            assertEquals(ConnectionMode.WebVpn, session.fallback()?.mode)
            val identity = session.identity
            session.fallback()
            assertEquals(identity, session.identity, "并发的直连失败不应反复作废代理请求")
        } finally { session.close() }
        val restoredFixture = GatewayFixture()
        val restored = WebVpnSession(persistence, restoredFixture.router, restoredFixture.engine())
        try {
            restored.setPolicy(ConnectionPolicy.Proxy)
            assertEquals(ConnectionMode.WebVpn, restored.connection().mode)
            assertEquals(WebVpnStage.Connected, restored.state.value.stage)
            assertFalse(restoredFixture.passwordSubmitted)
            restored.forgetAccount("demo-user")
            assertFailsWith<WebVpnLoginRequiredException> { restored.connection() }
        } finally { restored.close() }
    }

    @Test fun supportsCaptchaBeforeOtpAndClearsCancelledChallenges() = runTest {
        val fixture = GatewayFixture(captcha = true)
        val persistence = MemoryVpnPersistence()
        val session = WebVpnSession(persistence, fixture.router, fixture.engine())
        try {
            session.begin("demo-user", "fake-password")
            assertEquals(WebVpnStage.Captcha, session.state.value.stage)
            assertNotNull(session.state.value.captcha)
            assertFalse(fixture.passwordSubmitted)
            session.submitCaptcha("abcd")
            assertEquals(WebVpnStage.Otp, session.state.value.stage)
            session.cancelChallenge()
            assertEquals(WebVpnStage.Disconnected, session.state.value.stage)
            assertTrue(session.cookies.snapshot().isEmpty())
            assertNull(persistence.account)
            session.begin("demo-user", "fake-password")
            assertEquals(WebVpnStage.Captcha, session.state.value.stage)
            session.setPolicy(ConnectionPolicy.Direct)
            assertEquals(WebVpnStage.Disconnected, session.state.value.stage)
            assertFailsWith<IllegalStateException> { session.submitCaptcha("abcd") }
        } finally { session.close() }
    }

    @Test fun forcedDirectDoesNotFallbackAndExpiredGatewayRequiresUserAuthentication() = runTest {
        val persistence = MemoryVpnPersistence()
        val fixture = GatewayFixture()
        val session = WebVpnSession(persistence, fixture.router, fixture.engine())
        try {
            session.setPolicy(ConnectionPolicy.Direct)
            assertNull(session.fallback())
            session.setPolicy(ConnectionPolicy.Proxy)
            assertFailsWith<WebVpnLoginRequiredException> { session.connection() }
            assertFalse(fixture.passwordSubmitted)
        } finally { session.close() }
    }
}

internal class MemoryVpnPersistence : WebVpnPersistence {
    var selectedPolicy = ConnectionPolicy.Automatic
    var account: String? = null
    var cookies: String? = null
    override suspend fun load() = SavedWebVpnConnection(selectedPolicy, account, cookies)
    override suspend fun policy(value: ConnectionPolicy) { selectedPolicy = value }
    override suspend fun save(account: String, cookies: String) { this.account = account; this.cookies = cookies }
    override suspend fun forget(account: String?) { if (account == this.account) { this.account = null; cookies = null } }
}

private class GatewayFixture(private val captcha: Boolean = false) {
    val router = WebVpnUrlRouter()
    var passwordSubmitted = false
    var otpAccepted = false
    val formServices = mutableListOf<String?>()
    fun engine() = MockEngine { request ->
        assertEquals(router.gateway.host, request.url.host, "认证链不能跳回直连地址")
        val logical = router.original(request.url)
        val path = logical.encodedPath
        val html = headersOf(HttpHeaders.ContentType, "text/html;charset=UTF-8")
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        when {
            path == "/rump_frontend/login/" -> respond("""<html><a id="url" href="http://authserver.gdufe.edu.cn/authserver/login?service=${router.callback}">登录</a></html>""",
                headers = headersOf(HttpHeaders.ContentType to listOf("text/html"), HttpHeaders.SetCookie to listOf("my_vpn_ticket=preauth; Domain=.gdufe.edu.cn; Path=/; Secure; HttpOnly")))
            path == "/authserver/login" && request.method == HttpMethod.Get && !otpAccepted -> respond("""
                <form id="pwdFromId" action="/authserver/login">
                <input type="hidden" name="execution" id="execution" value="e1s1">
                <input type="hidden" id="pwdEncryptSalt" value="0123456789abcdef">
                <input type="hidden" name="_eventId" value="submit"></form>
            """, headers = html)
            path == "/authserver/checkNeedCaptcha.htl" -> respond("""{"isNeed":$captcha}""", headers = json)
            path == "/authserver/getCaptcha.htl" -> respond(byteArrayOf(1, 2, 3), headers = headersOf(HttpHeaders.ContentType, "image/png"))
            path == "/authserver/login" && request.method == HttpMethod.Post -> {
                passwordSubmitted = true
                val fields = parseQueryString((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
                assertEquals("demo-user", fields["username"])
                assertTrue(fields["password"].orEmpty().length > 80)
                if (captcha) assertEquals("abcd", fields["captcha"])
                formServices += request.url.parameters["service"]
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/authserver/reAuthCheck/reAuth"))
            }
            path == "/authserver/reAuthCheck/reAuth" -> respond("<html><input name=otpCode>reAuthSubmit</html>", headers = html)
            path == "/authserver/reAuthCheck/reAuthSubmit.do" -> {
                val fields = parseQueryString((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
                formServices += fields["service"]
                otpAccepted = fields["otpCode"] == "123456"
                respond(if (otpAccepted) """{"code":"reAuth_success","msg":"ok"}""" else """{"code":"failed","msg":"invalid"}""", headers = json)
            }
            path == "/authserver/login" && otpAccepted -> respond("", HttpStatusCode.Found,
                headersOf(HttpHeaders.Location, "${router.callback}?ticket=FAKE-TICKET"))
            path == "/rump_frontend/loginFromCas/" -> respond("<html>ok</html>", headers = headersOf(
                HttpHeaders.ContentType to listOf("text/html"), HttpHeaders.SetCookie to listOf("my_vpn_ticket=authenticated; Domain=.gdufe.edu.cn; Path=/; Secure; HttpOnly")))
            path == "/rump_frontend/getHomeParam/" -> respond(if (request.headers[HttpHeaders.Cookie].orEmpty().contains("my_vpn_ticket=authenticated"))
                """{"code":0}""" else """{"code":11}""", headers = json)
            else -> error("Unexpected fixture route: $path")
        }
    }
}
