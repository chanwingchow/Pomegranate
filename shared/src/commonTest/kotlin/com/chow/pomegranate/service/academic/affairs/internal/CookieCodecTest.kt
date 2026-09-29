package com.chow.pomegranate.service.academic.affairs.internal

import io.ktor.http.Cookie
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cookie 编解码测试。 */
class CookieCodecTest {
    /** 往返编解码后字段保持一致。 */
    @Test
    fun roundTripKeepsCookieFields() {
        val cookies = listOf(
            Cookie(
                name = "JSESSIONID",
                value = "A1B2C3",
                domain = "jwxt.gdufe.edu.cn",
                path = "/jsxsd",
                secure = true,
                httpOnly = true,
            ),
            Cookie(
                name = "SERVERID",
                value = "node-1",
                domain = "jwxt.gdufe.edu.cn",
            ),
        )

        val decoded = decodeCookies(encodeCookies(cookies))

        assertEquals(2, decoded.size)
        assertEquals("JSESSIONID", decoded[0].name)
        assertEquals("A1B2C3", decoded[0].value)
        assertEquals("jwxt.gdufe.edu.cn", decoded[0].domain)
        assertEquals("/jsxsd", decoded[0].path)
        assertTrue(decoded[0].secure)
        assertTrue(decoded[0].httpOnly)
        assertEquals("SERVERID", decoded[1].name)
        assertEquals("node-1", decoded[1].value)
    }

    /** 值中带有保留字符时也能正确还原。 */
    @Test
    fun roundTripEscapesReservedCharacters() {
        val cookies = listOf(
            Cookie(
                name = "token",
                value = "a=b&c\td\ne/f",
                domain = "example.com",
            ),
        )

        val decoded = decodeCookies(encodeCookies(cookies))

        assertEquals(1, decoded.size)
        assertEquals("a=b&c\td\ne/f", decoded[0].value)
    }

    /** 空内容与脏数据不会抛异常。 */
    @Test
    fun tolerateEmptyAndMalformedInput() {
        assertEquals(emptyList(), decodeCookies(""))
        assertEquals(emptyList(), decodeCookies("   "))
        assertEquals(emptyList(), decodeCookies("broken\tline"))
    }
}
