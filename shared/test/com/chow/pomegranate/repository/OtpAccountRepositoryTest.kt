package com.chow.pomegranate.repository

import com.chow.pomegranate.data.local.OtpAccountEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** OTP 账户仓库测试。 */
class OtpAccountRepositoryTest {
    /** 保存前规范化账户字段。 */
    @Test
    fun normalizeAccount() {
        val account = normalizeOtpAccount(
            OtpAccountEntity(
                issuer = " Service ",
                accountName = " user@example.com ",
                secret = "jbsw y3dp-ehpk3pxp",
            ),
        )

        assertEquals("Service", account.issuer)
        assertEquals("user@example.com", account.accountName)
        assertEquals("JBSWY3DPEHPK3PXP", account.secret)
    }

    /** 拒绝无效密钥和参数。 */
    @Test
    fun rejectInvalidAccount() {
        assertFailsWith<IllegalArgumentException> {
            normalizeOtpAccount(OtpAccountEntity(issuer = "Service", accountName = "user", secret = "!"))
        }
        assertFailsWith<IllegalArgumentException> {
            normalizeOtpAccount(
                OtpAccountEntity(
                    issuer = "Service",
                    accountName = "user",
                    secret = "JBSWY3DPEHPK3PXP",
                    digits = 5,
                ),
            )
        }
    }
}
