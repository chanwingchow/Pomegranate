package com.chow.pomegranate.network

import com.chow.pomegranate.session.CookieStorage
import com.chow.pomegranate.service.academic.affairs.internal.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WebVpnCookieTest {
    @Test fun isolatesSameNameCookiesByPathAndHonorsDomainScope() = runTest {
        val store = CookieStorage()
        val gateway = Url("https://sec.gdufe.edu.cn/rump_frontend/login/")
        store.addCookie(gateway, Cookie("JSESSIONID", "gateway", path = "/rump_frontend"))
        store.addCookie(gateway, Cookie("JSESSIONID", "upstream", path = "/webvpn"))
        store.addCookie(gateway, Cookie("redirectTo", "target", domain = ".gdufe.edu.cn", path = "/"))
        assertEquals(setOf("gateway", "target"), store.get(gateway).map { it.value }.toSet())
        assertEquals(setOf("upstream", "target"), store.get(Url("https://sec.gdufe.edu.cn/webvpn/a")).map { it.value }.toSet())
        assertEquals(listOf("target"), store.get(Url("https://authserver.gdufe.edu.cn/authserver/login")).map { it.value })
        val restored = CookieStorage(); restored.restore(decodeCookies(encodeCookies(store.snapshot())))
        assertEquals(store.get(gateway), restored.get(gateway))
        assertTrue(restored.get(Url("https://evilgdufe.edu.cn/")).isEmpty())
    }
    @Test fun expiresDeletesAndRespectsSecureAndPathBoundaries() = runTest {
        var now = 1_000L
        val store = CookieStorage { now }
        val url = Url("https://sec.gdufe.edu.cn/webvpn/a")
        store.addCookie(url, Cookie("ticket", "one", path = "/webvpn", secure = true, maxAge = 2))
        assertTrue(store.get(Url("http://sec.gdufe.edu.cn/webvpn/a")).isEmpty())
        assertTrue(store.get(Url("https://sec.gdufe.edu.cn/webvpnevil/a")).isEmpty())
        now = 3_001L; assertTrue(store.get(url).isEmpty())
        store.addCookie(url, Cookie("ticket", "two", path = "/"))
        store.addCookie(url, Cookie("ticket", "", path = "/", maxAge = 0))
        assertTrue(store.get(url).isEmpty())
    }
}
