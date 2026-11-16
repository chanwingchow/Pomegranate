package com.chow.pomegranate.network

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64

/** 与学校 encryptAES/Python 脚本一致：64 字符前缀，CBC+PKCS7，仅 Base64 密文。 */
internal object CasPasswordCipher {
    private const val alphabet = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
    private fun randomString(size: Int) = CharArray(size) { alphabet[CryptographyRandom.Default.nextInt(alphabet.length)] }.concatToString()

    @OptIn(DelicateCryptographyApi::class)
    suspend fun encrypt(password: String, salt: String, prefix: String = randomString(64), iv: String = randomString(16)): String {
        require(prefix.length == 64 && iv.encodeToByteArray().size == 16) { "无效的认证加密参数。" }
        val key = CryptographyProvider.Default.get(AES.CBC).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, salt.trim().encodeToByteArray())
        return Base64.Default.encode(key.cipher().encryptWithIv(iv.encodeToByteArray(), (prefix + password).encodeToByteArray()))
    }
}
