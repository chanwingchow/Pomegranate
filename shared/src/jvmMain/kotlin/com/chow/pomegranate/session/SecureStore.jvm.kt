package com.chow.pomegranate.session

import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** 加密变换。 */
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** GCM 认证标签长度（位）。 */
private const val TAG_LENGTH_BITS = 128

/** 初始化向量长度（字节）。 */
private const val IV_LENGTH_BYTES = 12

actual fun createSecureStore(): SecureStore = JvmSecureStore()

/**
 * 桌面端的安全存储。
 *
 * JVM 没有系统级密钥库，这里使用本机密钥文件配合 AES-GCM 加密，
 * 文件权限限制为当前用户可读写。
 */
private class JvmSecureStore : SecureStore {
    /** 存储目录。 */
    private val directory = File(System.getProperty("user.home"), ".pomegranate").apply { mkdirs() }

    /** 密钥文件。 */
    private val keyFile = File(directory, "secure_store.key")

    /** 数据文件。 */
    private val dataFile = File(directory, "secure_store.dat")

    /** 读写锁，避免并发 put/remove 互相覆盖。 */
    private val lock = Any()

    /**
     * 解密失败时保留的最后一份可用条目。
     *
     * 数据文件损坏（密钥丢失、文件被截断）时不能按空 map 继续写回：
     * 那会把剩余凭据一并抹掉。这里退回内存副本，至少保证本次会话仍可用。
     */
    private var cachedEntries: Map<String, String>? = null

    override fun put(
        key: String,
        value: String,
    ) {
        synchronized(lock) {
            val entries = readEntries().toMutableMap()
            entries[key] = value
            writeEntries(entries)
        }
    }

    override fun get(key: String): String? = synchronized(lock) { readEntries()[key] }

    override fun remove(key: String) {
        synchronized(lock) {
            val entries = readEntries().toMutableMap()
            if (entries.remove(key) != null) writeEntries(entries)
        }
    }

    /** 读取并解密全部条目。 */
    private fun readEntries(): Map<String, String> {
        if (!dataFile.exists()) return emptyMap()

        val decrypted = runCatching {
            val payload = dataFile.readBytes()
            require(payload.size > IV_LENGTH_BYTES) { "Invalid secure store payload." }

            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherText = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }

            cipher.doFinal(cipherText)
                .decodeToString()
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val separatorIndex = line.indexOf('\t')
                    if (separatorIndex <= 0) null
                    else line.substring(0, separatorIndex) to decodeValue(line.substring(separatorIndex + 1))
                }
                .toMap()
        }

        return decrypted.getOrElse {
            // 解密失败：不要回退成空 map，否则紧接着的写回会丢光其余凭据
            cachedEntries ?: emptyMap()
        }.also { cachedEntries = it }
    }

    /** 加密并写入全部条目。 */
    private fun writeEntries(entries: Map<String, String>) {
        val plainText = entries.entries.joinToString("\n") { (key, value) ->
            "$key\t${encodeValue(value)}"
        }

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }

        val cipherText = cipher.doFinal(plainText.encodeToByteArray())

        // 先写临时文件再替换，避免写到一半崩溃把原文件截断
        val temporary = File(directory, "secure_store.dat.tmp")
        temporary.writeBytes(cipher.iv + cipherText)
        restrictPermissions(temporary)
        if (!temporary.renameTo(dataFile)) {
            dataFile.writeBytes(temporary.readBytes())
            temporary.delete()
            restrictPermissions(dataFile)
        }

        cachedEntries = entries
    }

    /** 收紧文件权限为仅当前用户可读写。 */
    private fun restrictPermissions(file: File) {
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
    }

    /** 返回本机密钥，不存在时创建。 */
    private fun secretKey(): SecretKey {
        if (keyFile.exists()) {
            return SecretKeySpec(keyFile.readBytes(), "AES")
        }

        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        keyFile.writeBytes(key.encoded)
        keyFile.setReadable(false, false)
        keyFile.setReadable(true, true)

        return key
    }
}

/** 转义换行，避免破坏条目分隔。 */
private fun encodeValue(value: String): String = Base64.getEncoder().encodeToString(value.encodeToByteArray())

/** 还原转义值。 */
private fun decodeValue(value: String): String = runCatching {
    Base64.getDecoder().decode(value).decodeToString()
}.getOrDefault("")
