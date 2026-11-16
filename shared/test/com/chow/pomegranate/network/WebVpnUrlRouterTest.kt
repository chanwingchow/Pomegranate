package com.chow.pomegranate.network

import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WebVpnUrlRouterTest {
    @Test fun matchesTheGatewaysPublishedVectors() = runTest {
        val router = WebVpnUrlRouter()
        assertEquals("LjIwMy4yMTUuMTY1LjE2MQ==", router.encode("http"))
        assertEquals("LjIwNS4yMTguMTY5LjE2NS45NC4xNTMuMTk5LjE2NS4xOTkuMTUxLjk0LjE1OC4xNTEuMjE5Ljk5LjE0OS4yMTE=", router.encode("jwxt.gdufe.edu.cn"))
        assertNotEquals(router.encode("http"), WebVpnUrlRouter("sec.gdufe.edu.cn").encode("http"))
    }
    @Test fun keepsAbsolutePathsEncodedQueriesAndDuplicateParameters() = runTest {
        val router = WebVpnUrlRouter()
        val url = Url("http://jwxt.gdufe.edu.cn/jsxsd/x?a=%E4%B8%AD&x=a%2Bb%26c&dup=1&dup=2")
        val proxy = router.route(url, ConnectionMode.WebVpn)
        assertEquals("sec.gdufe.edu.cn", proxy.host)
        assertTrue(proxy.encodedPath.endsWith("/jsxsd/x"))
        assertEquals(listOf("1", "2"), proxy.parameters.getAll("dup"))
        assertEquals("a+b&c", proxy.parameters["x"])
        assertEquals(url, router.original(proxy))
        assertEquals(proxy, router.route(proxy, ConnectionMode.WebVpn))
        assertEquals(url, router.route(url, ConnectionMode.Direct))
        assertEquals(Url("https://jwc.gdufe.edu.cn/test"), router.route(Url("https://jwc.gdufe.edu.cn/test"), ConnectionMode.WebVpn))
    }
    @Test fun rewritesRootRelativeRedirectsAgainstTheOriginalSite() = runTest {
        val router = WebVpnUrlRouter()
        val proxy = router.route(Url("http://jwxt.gdufe.edu.cn/jsxsd/a/start"), ConnectionMode.WebVpn)
        assertEquals("/jsxsd/login", router.original(router.redirect(proxy, "/jsxsd/login", ConnectionMode.WebVpn)).encodedPath)
        assertEquals("/jsxsd/a/next", router.original(router.redirect(proxy, "next", ConnectionMode.WebVpn)).encodedPath)
        assertEquals("/rump_frontend/login/", router.redirect(proxy, "/rump_frontend/login/", ConnectionMode.WebVpn).encodedPath)
    }
}
