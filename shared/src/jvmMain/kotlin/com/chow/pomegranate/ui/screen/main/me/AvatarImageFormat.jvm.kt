package com.chow.pomegranate.ui.screen.main.me

import java.io.InputStream

/** 读取图片文件头所需的最大字节数。 */
private const val AVATAR_SIGNATURE_BYTES = 16

/**
 * 从图片字节的文件头判断格式，用于给本地头像副本挑一个真实的后缀。
 *
 * 只做识别，不做解码或重编码：PNG 这类无损格式原样存下来即可，透明通道也不会丢。
 */
internal fun detectImageExtension(bytes: ByteArray): String {
    fun matches(
        offset: Int,
        vararg signature: Int,
    ): Boolean {
        if (bytes.size < offset + signature.size) return false
        return signature.withIndex().all { (index, byte) ->
            bytes[offset + index].toInt() and 0xFF == byte
        }
    }

    return when {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        matches(0, 0x89, 0x50, 0x4E, 0x47) -> "png"
        // JPEG: FF D8 FF
        matches(0, 0xFF, 0xD8, 0xFF) -> "jpg"
        // GIF: GIF8
        matches(0, 0x47, 0x49, 0x46, 0x38) -> "gif"
        // WebP: RIFF....WEBP
        matches(0, 0x52, 0x49, 0x46, 0x46) && matches(8, 0x57, 0x45, 0x42, 0x50) -> "webp"
        // HEIC/HEIF: ....ftyp
        matches(4, 0x66, 0x74, 0x79, 0x70) -> "heic"
        else -> DEFAULT_AVATAR_EXTENSION
    }
}

/**
 * 只读取文件头来判断格式。
 *
 * 头像文件可能接近 10 MB，没必要整份读进内存，而各格式的特征字节都在前 16 字节内。
 */
internal fun detectImageExtension(input: InputStream): String =
    input.use { stream ->
        val header = ByteArray(AVATAR_SIGNATURE_BYTES)
        var read = 0
        while (read < header.size) {
            val count = stream.read(header, read, header.size - read)
            if (count <= 0) break
            read += count
        }

        detectImageExtension(header.copyOf(read))
    }
