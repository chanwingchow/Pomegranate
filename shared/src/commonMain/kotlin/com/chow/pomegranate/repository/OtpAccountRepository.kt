package com.chow.pomegranate.repository

import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.OtpAccountDao
import com.chow.pomegranate.data.local.OtpAccountEntity
import com.chow.pomegranate.tool.otp.TotpGenerator
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/** OTP 账户仓库。 */
@Single
class OtpAccountRepository(database: AppDatabase) {
    private val dao: OtpAccountDao = database.otpAccountDao()

    /** 观察全部账户。 */
    fun observeAll(): Flow<List<OtpAccountEntity>> = dao.observeAll()

    /** 返回指定账户。 */
    suspend fun get(id: Long): OtpAccountEntity? = dao.account(id)

    /** 保存账户，并返回账户 ID。 */
    suspend fun save(account: OtpAccountEntity): Long {
        val normalized = normalizeOtpAccount(account)
        return dao.save(normalized)
    }

    /** 删除账户。 */
    suspend fun delete(account: OtpAccountEntity) = dao.delete(account)
}

/** 规范化并验证 OTP 账户。 */
internal fun normalizeOtpAccount(account: OtpAccountEntity): OtpAccountEntity {
    val normalized = account.copy(
        issuer = account.issuer.trim(),
        accountName = account.accountName.trim(),
        secret = account.secret.trim().uppercase().filterNot { it == ' ' || it == '-' },
    )
    require(normalized.issuer.isNotEmpty()) { "服务名称不能为空。" }
    require(normalized.accountName.isNotEmpty()) { "账户名称不能为空。" }
    TotpGenerator(
        secret = normalized.secret,
        digits = normalized.digits,
        periodSeconds = normalized.periodSeconds,
    )
    return normalized
}
