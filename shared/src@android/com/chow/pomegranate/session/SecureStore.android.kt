package com.chow.pomegranate.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.koin.mp.KoinPlatform
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 密钥库别名。 */
private const val KEY_ALIAS = "com.chow.pomegranate.secure_store"

/** 偏好文件名。 */
private const val PREFERENCES_NAME = "secure_store"

/** 加密变换。 */
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** GCM 认证标签长度（位）。 */
private const val TAG_LENGTH_BITS = 128

/** 初始化向量长度（字节）。 */
private const val IV_LENGTH_BYTES = 12

actual fun createSecureStore(): SecureStore = AndroidSecureStore(
    context = KoinPlatform.getKoin().get<Context>(),
)

/**
 * 基于 `AndroidKeyStore` 的安全存储。
 *
 * 密钥由系统密钥库保护且不可导出，密文与初始化向量存放在私有 `SharedPreferences` 中。
 *
 * @param context 应用上下文
 */
private class AndroidSecureStore(context: Context) : SecureStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun put(
        key: String,
        value: String,
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }

        val cipherText = cipher.doFinal(value.encodeToByteArray())

        // 初始化向量与密文一并保存，以冒号分隔
        preferences.edit()
            .putString(key, "${cipher.iv.toBase64()}:${cipherText.toBase64()}")
            .apply()
    }

    override fun get(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null

        val separatorIndex = stored.indexOf(':')
        if (separatorIndex <= 0) {
            preferences.edit().remove(key).apply()
            return null
        }

        val iv = stored.substring(0, separatorIndex).fromBase64() ?: return null
        val cipherText = stored.substring(separatorIndex + 1).fromBase64() ?: return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
            cipher.doFinal(cipherText).decodeToString()
        }.getOrElse {
            // 密钥失效或数据损坏时清理，避免反复失败
            preferences.edit().remove(key).apply()
            null
        }
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    /** 返回密钥库中的密钥，不存在时创建。 */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }
}

/** 编码为 Base64。 */
private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

/** 解码 Base64，失败时返回 `null`。 */
private fun String.fromBase64(): ByteArray? = runCatching {
    Base64.decode(this, Base64.NO_WRAP)
}.getOrNull()
