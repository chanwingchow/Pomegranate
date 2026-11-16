package com.chow.pomegranate.tool.otp

import kotlin.test.Test
import kotlin.test.assertEquals

/** TOTP 生成器测试。 */
class TotpGeneratorTest {
    /** 验证 RFC 6238 SHA-1 测试向量。 */
    @Test
    fun generateRfc6238Code() {
        val generator = TotpGenerator(
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            digits = 8,
        )

        assertEquals("94287082", generator.generate(epochSeconds = 59))
    }
}
