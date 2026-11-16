package com.chow.pomegranate.tool.otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** otpauth:// 链接解析测试。 */
class OtpAuthUriTest {
    /** 解析标准链接，服务与账户从标签里拆分。 */
    @Test
    fun parseStandardUri() {
        val account = parseOtpAuthUri(
            "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        )

        assertEquals("Example", account?.issuer)
        assertEquals("alice@example.com", account?.accountName)
        assertEquals("JBSWY3DPEHPK3PXP", account?.secret)
        assertEquals(6, account?.digits)
        assertEquals(30L, account?.periodSeconds)
        assertEquals("SHA1", account?.algorithm)
    }

    /** 解析非默认位数与周期。 */
    @Test
    fun parseCustomDigitsAndPeriod() {
        val account = parseOtpAuthUri(
            "otpauth://totp/GitHub:bob?secret=ABCDEFGH&digits=8&period=60",
        )

        assertEquals(8, account?.digits)
        assertEquals(60L, account?.periodSeconds)
    }

    /** 中文服务名按 UTF-8 解码，不能乱码。 */
    @Test
    fun parseEncodedChineseIssuer() {
        val account = parseOtpAuthUri(
            "otpauth://totp/%E5%B9%BF%E8%B4%A2:202600000001" +
                "?secret=JBSWY3DPEHPK3PXP&issuer=%E5%B9%BF%E8%B4%A2",
        )

        assertEquals("广财", account?.issuer)
        assertEquals("202600000001", account?.accountName)
    }

    /** 密钥里的空格要去掉，并统一转大写。 */
    @Test
    fun normalizeSecret() {
        val account = parseOtpAuthUri(
            "otpauth://totp/Example:alice?secret=jbswy3dp ehpk3pxp",
        )

        assertEquals("JBSWY3DPEHPK3PXP", account?.secret)
    }

    /** 没有 issuer 参数时，服务名取标签冒号前的部分。 */
    @Test
    fun fallbackIssuerFromLabel() {
        val account = parseOtpAuthUri("otpauth://totp/GDUFE:202600000001?secret=ABCDEFGH")

        assertEquals("GDUFE", account?.issuer)
        assertEquals("202600000001", account?.accountName)
    }

    /** 标签里没有冒号时，账户名就是整段标签。 */
    @Test
    fun accountFromPlainLabel() {
        val account = parseOtpAuthUri("otpauth://totp/alice@example.com?secret=ABCDEFGH")

        assertEquals("", account?.issuer)
        assertEquals("alice@example.com", account?.accountName)
    }

    /** 缺少密钥时解析失败。 */
    @Test
    fun rejectMissingSecret() {
        assertNull(parseOtpAuthUri("otpauth://totp/Example:alice?issuer=Example"))
    }

    /** 非 otpauth 链接一律不认。 */
    @Test
    fun rejectNonOtpAuthText() {
        assertNull(parseOtpAuthUri("https://example.com"))
        assertNull(parseOtpAuthUri("JBSWY3DPEHPK3PXP"))
        assertNull(parseOtpAuthUri(""))
    }

    /** 只支持 totp，hotp 不认（本项目生成器基于时间）。 */
    @Test
    fun rejectHotp() {
        assertNull(parseOtpAuthUri("otpauth://hotp/Example:alice?secret=ABCDEFGH&counter=1"))
    }

    /** 非法位数回落到默认值。 */
    @Test
    fun fallbackInvalidDigits() {
        assertEquals(6, parseOtpAuthUri("otpauth://totp/a?secret=ABCDEFGH&digits=99")?.digits)
        assertEquals(6, parseOtpAuthUri("otpauth://totp/a?secret=ABCDEFGH&digits=abc")?.digits)
    }

    /** 非法周期回落到默认值。 */
    @Test
    fun fallbackInvalidPeriod() {
        assertEquals(30L, parseOtpAuthUri("otpauth://totp/a?secret=ABCDEFGH&period=0")?.periodSeconds)
        assertEquals(30L, parseOtpAuthUri("otpauth://totp/a?secret=ABCDEFGH&period=x")?.periodSeconds)
    }
}
