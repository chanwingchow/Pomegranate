package com.chow.pomegranate.network

import com.chow.pomegranate.session.CookieStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ServiceTransportTest {
    @Test fun bindsAccountAfterTheSessionGateFinishesAutomaticLogin() = runTest {
        val router = WebVpnUrlRouter()
        var identity = "anonymous"
        val client = HttpClient(MockEngine { respond("ok") }) {
            install(createClientPlugin("TestSessionGate") {
                on(Send) { request ->
                    identity = "signed-in"
                    proceed(request)
                }
            })
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct, identity) }, { identity })
        }
        try { assertEquals("ok", client.get("http://jwxt.gdufe.edu.cn/test").bodyAsText()) }
        finally { client.close() }
    }
    @Test fun cancellationCertificateErrorsAndUnrelatedHostsNeverFallback() = runTest {
        val router = WebVpnUrlRouter()
        class CertificateFailure : IOException("certificate rejected")
        var fallback = 0
        val cases = listOf(
            "http://jwxt.gdufe.edu.cn/test" to CancellationException("user left"),
            "https://jwxt.gdufe.edu.cn/test" to IOException("TLS", CertificateFailure()),
            "https://example.org/test" to IOException("unreachable"),
        )
        for ((url, error) in cases) {
            var calls = 0
            val client = HttpClient(MockEngine { calls++; throw error }) {
                installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct) }, onDirectUnavailable = { fallback++; ServiceConnection(ConnectionMode.WebVpn) })
            }
            try { assertFails { client.get(url) }; assertEquals(1, calls) } finally { client.close() }
        }
        assertEquals(0, fallback)
    }

    @Test fun usesDefaultAddressForRelativeRequestsAndPreservesReadOnlySearchBody() = runTest {
        val router = WebVpnUrlRouter()
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            assertEquals("/jsxsd/xsxkkc/xsxkXxxk", router.original(request.url).encodedPath)
            if (!router.isGateway(request.url)) throw IOException("unreachable")
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("search", (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString())
            respond("results")
        }) {
            defaultRequest { url("http://jwxt.gdufe.edu.cn") }
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct) }, onDirectUnavailable = { ServiceConnection(ConnectionMode.WebVpn) })
        }
        try {
            assertEquals("results", client.post("/jsxsd/xsxkkc/xsxkXxxk") { setBody("search") }.bodyAsText())
            assertEquals(2, calls)
        } finally { client.close() }
    }

    @Test fun proxyRedirectsStripTokensWhenTheOriginalHostChanges() = runTest {
        val router = WebVpnUrlRouter()
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            if (calls == 1) respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://2ketang.gdufe.edu.cn/test"))
            else {
                assertEquals("2ketang.gdufe.edu.cn", router.original(request.url).host)
                assertNull(request.headers[HttpHeaders.Authorization])
                assertNull(request.headers["X-token"])
                respond("ok")
            }
        }) { installServiceTransport(router, { ServiceConnection(ConnectionMode.WebVpn) }) }
        try {
            assertEquals("ok", client.get("http://jwxt.gdufe.edu.cn/test") {
                header(HttpHeaders.Authorization, "Bearer fake"); header("X-token", "fake")
            }.bodyAsText())
            assertEquals(2, calls)
        } finally { client.close() }
    }

    @Test fun requestTimeoutCanFallbackWithoutReusingTheCancelledRequest() = runTest {
        val router = WebVpnUrlRouter()
        var calls = 0
        val client = HttpClient(MockEngine { request ->
            calls++
            if (request.url.host == "jwxt.gdufe.edu.cn") delay(5_000)
            respond("ok")
        }) {
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct) },
                onDirectUnavailable = { ServiceConnection(ConnectionMode.WebVpn) })
            install(HttpTimeout) { requestTimeoutMillis = 200 }
        }
        try {
            assertEquals("ok", client.get("http://jwxt.gdufe.edu.cn/test").bodyAsText())
            assertEquals(2, calls)
        } finally { client.close() }
    }
    @Test fun automaticallyRetriesReadWithGatewayCookieInsteadOfDirectCookie() = runTest {
        val router = WebVpnUrlRouter()
        val direct = CookieStorage(); val vpn = CookieStorage()
        direct.addCookie(Url("http://jwxt.gdufe.edu.cn/"), Cookie("JSESSIONID", "direct", path = "/"))
        vpn.addCookie(router.gateway, Cookie("my_vpn_ticket", "proxy", path = "/"))
        var calls = 0; var fallback = 0; var identity = "direct-owner"
        val engine = MockEngine { request ->
            calls++
            if (request.url.host == "jwxt.gdufe.edu.cn") throw IOException("simulated unreachable")
            assertEquals(router.gateway.host, request.url.host)
            assertEquals("my_vpn_ticket=proxy", request.headers[HttpHeaders.Cookie])
            respond("ok")
        }
        val client = HttpClient(engine) {
            install(HttpCookies) { storage = ServiceCookieStorage(direct, vpn, router) }
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct, identity) }, { identity }, {
                fallback++; identity = "proxy-owner"; ServiceConnection(ConnectionMode.WebVpn, identity)
            })
        }
        try { assertEquals("ok", client.get("http://jwxt.gdufe.edu.cn/jsxsd/x").bodyAsText()); assertEquals(2, calls); assertEquals(1, fallback) }
        finally { client.close() }
    }
    @Test fun neverReplaysSelectionDropOrCredentialPosts() = runTest {
        val router = WebVpnUrlRouter()
        var calls = 0; var fallback = 0
        val client = HttpClient(MockEngine { calls++; throw IOException("unreachable") }) {
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct) }, onDirectUnavailable = { fallback++; ServiceConnection(ConnectionMode.WebVpn) })
        }
        try {
            assertFailsWith<ConnectionChangedException> { client.get("http://jwxt.gdufe.edu.cn/jsxsd/xsxkjg/xstkOper") }
            assertFailsWith<ConnectionChangedException> { client.get("http://jwxt.gdufe.edu.cn/jsxsd/xsxkkc/xxxkOper") }
            assertFailsWith<ConnectionChangedException> { client.post("http://jwxt.gdufe.edu.cn/jsxsd/xk/LoginToXkLdap") }
            assertEquals(3, calls); assertEquals(3, fallback)
        } finally { client.close() }
    }
    @Test fun loginFailuresDoNotTriggerFallbackAndGatewayExpiryDoesNotReplay() = runTest {
        val router = WebVpnUrlRouter()
        var fallback = 0
        val client = HttpClient(MockEngine { respond("wrong password", HttpStatusCode.Unauthorized) }) {
            installServiceTransport(router, { ServiceConnection(ConnectionMode.Direct) }, onDirectUnavailable = { fallback++; null })
        }
        try { assertEquals(401, client.get("http://jwxt.gdufe.edu.cn/test").status.value); assertEquals(0, fallback) } finally { client.close() }
        var calls = 0; var expired = false
        val proxy = HttpClient(MockEngine { calls++; respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "/rump_frontend/login/")) }) {
            installServiceTransport(router, { ServiceConnection(ConnectionMode.WebVpn) }, onExpired = { expired = true })
        }
        try {
            assertFailsWith<WebVpnLoginRequiredException> { proxy.get("http://jwxt.gdufe.edu.cn/test") }
            assertTrue(expired); assertEquals(1, calls)
        } finally { proxy.close() }
    }
}
