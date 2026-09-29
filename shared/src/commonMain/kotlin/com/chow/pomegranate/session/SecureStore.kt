package com.chow.pomegranate.session

/**
 * 平台安全存储。
 *
 * 用于保存密码等敏感凭据，避免与业务数据一同落库：
 * - Android：`AndroidKeyStore` 生成 AES-GCM 密钥，密文存入 `SharedPreferences`；
 * - iOS：Keychain；
 * - JVM：本机密钥文件 + AES-GCM 加密文件。
 */
interface SecureStore {
    /** 保存 [value]。 */
    fun put(
        key: String,
        value: String,
    )

    /** 返回 [key] 对应的值，不存在或解密失败时返回 `null`。 */
    fun get(key: String): String?

    /** 删除 [key]。 */
    fun remove(key: String)
}

/** 返回当前平台的安全存储。 */
expect fun createSecureStore(): SecureStore
