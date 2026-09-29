package com.chow.pomegranate.session

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cookie 仓库测试。
 *
 * 重点覆盖"保存在本地 → 恢复后仍能按 host 取回"这条链路：
 * 早先 restore 按 domain 归档、get 按 host 查，导致持久化的会话永远恢复不了，
 * 自动登录形同虚设。
 */
class CookieStorageTest {
    private val host = "jwxt.gdufe.edu.cn"
    private val url = Url("http://$host/jsxsd/xk/LoginToXk")

    @Test
    fun restoredCookiesAreReadableByHost() = runBlocking {
        val storage = CookieStorage()
        storage.addCookie(url, Cookie(name = "JSESSIONID", value = "abc"))

        val snapshot = storage.snapshot()

        val restored = CookieStorage()
        restored.restore(snapshot)

        val cookies = restored.get(url)
        assertEquals(1, cookies.size)
        assertEquals("abc", cookies.single().value)
    }

    @Test
    fun cookiesWithoutDomainStillMatchTheRequestHost() = runBlocking {
        val storage = CookieStorage()

        // 服务端没下发 Domain 的会话 Cookie 必须补上 host，
        // 否则按 host 归档/查询的持久化链路会把它丢掉
        storage.addCookie(url, Cookie(name = "JSESSIONID", value = "abc"))

        val cookie = storage.snapshot().single()
        assertEquals(host, cookie.domain, "缺少 Domain 的会话 Cookie 应补上请求 host")
    }

    @Test
    fun sameNameCookieKeepsTheLatestValue() = runBlocking {
        val storage = CookieStorage()
        storage.addCookie(url, Cookie(name = "JSESSIONID", value = "old"))
        storage.addCookie(url, Cookie(name = "JSESSIONID", value = "new"))

        val cookies = storage.get(url)
        assertEquals(1, cookies.size)
        assertEquals("new", cookies.single().value)
    }

    @Test
    fun clearRemovesEverything() = runBlocking {
        val storage = CookieStorage()
        storage.addCookie(url, Cookie(name = "JSESSIONID", value = "abc"))

        storage.clear()

        assertTrue(storage.get(url).isEmpty())
    }
}
