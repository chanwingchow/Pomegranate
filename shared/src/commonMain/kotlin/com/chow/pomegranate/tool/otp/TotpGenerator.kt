package com.chow.pomegranate.tool.otp

import kotlin.time.Clock

/**
 * Time-Based One-Time Password 生成器。
 *
 * 算法遵循 RFC 4226 与 RFC 6238，默认生成 6 位、30 秒有效的 SHA-1 密码。
 */
class TotpGenerator(
    secret: String,
    private val digits: Int = 6,
    private val periodSeconds: Long = 30,
) {
    private val secretBytes = decodeBase32(secret)

    init {
        require(digits in 6..8) { "TOTP digits must be between 6 and 8." }
        require(periodSeconds > 0) { "TOTP period must be positive." }
        require(secretBytes.isNotEmpty()) { "TOTP secret must not be empty." }
    }

    /** 生成当前时间对应的密码。 */
    fun generate(): String = generate(Clock.System.now().epochSeconds)

    /**
     * 生成 [epochSeconds] 对应的密码。
     */
    fun generate(epochSeconds: Long): String {
        val counter = epochSeconds / periodSeconds
        val message = ByteArray(Long.SIZE_BYTES) { index ->
            (counter ushr (Byte.SIZE_BITS * (Long.SIZE_BYTES - index - 1))).toByte()
        }
        val hash = hmacSha1(secretBytes, message)
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        val divisor = powersOfTen[digits]

        return (binary % divisor).toString().padStart(digits, '0')
    }

    /** 返回当前密码剩余秒数。 */
    fun remainingSeconds(epochSeconds: Long = Clock.System.now().epochSeconds): Long {
        return periodSeconds - epochSeconds % periodSeconds
    }

    private companion object {
        val powersOfTen = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)
    }
}

/** 解码 Base32 密钥。 */
private fun decodeBase32(value: String): ByteArray {
    val normalized = value.uppercase().filterNot { it == ' ' || it == '-' || it == '=' }
    var buffer = 0
    var bitCount = 0
    val result = mutableListOf<Byte>()

    normalized.forEach { character ->
        val digit = when (character) {
            in 'A'..'Z' -> character - 'A'
            in '2'..'7' -> character - '2' + 26
            else -> throw IllegalArgumentException("Invalid Base32 character: $character")
        }
        buffer = (buffer shl 5) or digit
        bitCount += 5
        if (bitCount >= Byte.SIZE_BITS) {
            bitCount -= Byte.SIZE_BITS
            result += (buffer shr bitCount).toByte()
            buffer = buffer and ((1 shl bitCount) - 1)
        }
    }

    return result.toByteArray()
}

/** 返回 HMAC-SHA1。 */
private fun hmacSha1(key: ByteArray, message: ByteArray): ByteArray {
    val blockSize = 64
    val normalizedKey = if (key.size > blockSize) sha1(key) else key
    val paddedKey = normalizedKey.copyOf(blockSize)
    val innerPad = ByteArray(blockSize) { index -> (paddedKey[index].toInt() xor 0x36).toByte() }
    val outerPad = ByteArray(blockSize) { index -> (paddedKey[index].toInt() xor 0x5c).toByte() }
    return sha1(outerPad + sha1(innerPad + message))
}

/** 返回 SHA-1 摘要。 */
private fun sha1(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * Byte.SIZE_BITS
    val paddedSize = ((input.size + 9 + 63) / 64) * 64
    val data = ByteArray(paddedSize)
    input.copyInto(data)
    data[input.size] = 0x80.toByte()
    repeat(Long.SIZE_BYTES) { index ->
        data[data.lastIndex - index] = (bitLength ushr (Byte.SIZE_BITS * index)).toByte()
    }

    var h0 = 0x67452301
    var h1 = 0xefcdab89.toInt()
    var h2 = 0x98badcfe.toInt()
    var h3 = 0x10325476
    var h4 = 0xc3d2e1f0.toInt()
    val words = IntArray(80)

    data.asList().chunked(64).forEach { chunk ->
        repeat(16) { index ->
            val offset = index * 4
            words[index] = ((chunk[offset].toInt() and 0xff) shl 24) or
                ((chunk[offset + 1].toInt() and 0xff) shl 16) or
                ((chunk[offset + 2].toInt() and 0xff) shl 8) or
                (chunk[offset + 3].toInt() and 0xff)
        }
        for (index in 16 until 80) {
            words[index] = (words[index - 3] xor words[index - 8] xor words[index - 14] xor words[index - 16]).rotateLeft(1)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        repeat(80) { index ->
            val (function, constant) = when (index) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5a827999
                in 20..39 -> (b xor c xor d) to 0x6ed9eba1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8f1bbcdc.toInt()
                else -> (b xor c xor d) to 0xca62c1d6.toInt()
            }
            val temporary = a.rotateLeft(5) + function + e + constant + words[index]
            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temporary
        }
        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
    }

    return intArrayOf(h0, h1, h2, h3, h4).flatMap { value ->
        listOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
    }.toByteArray()
}
