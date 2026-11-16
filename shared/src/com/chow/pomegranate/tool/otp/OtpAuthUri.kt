package com.chow.pomegranate.tool.otp

/** otpauth:// 链接里解析出的账户信息。 */
data class OtpAuthAccount(
    val issuer: String,
    val accountName: String,
    val secret: String,
    val digits: Int,
    val periodSeconds: Long,
    val algorithm: String,
)

/** otpauth:// 协议前缀。 */
private const val OTPAUTH_SCHEME = "otpauth://"

/** 默认的哈希算法。 */
private const val DEFAULT_ALGORITHM = "SHA1"

/** 默认位数。 */
private const val DEFAULT_DIGITS = 6

/** 默认周期（秒）。 */
private const val DEFAULT_PERIOD_SECONDS = 30L

/** 支持的位数范围，与 [TotpGenerator] 保持一致。 */
private val SUPPORTED_DIGITS = 6..8

/**
 * 解析 otpauth:// 链接。
 *
 * 只支持基于时间的 `totp` 类型，链接缺少密钥或格式不对时返回 `null`。
 *
 * @param text 可能是完整链接，也可能是从二维码里复制出来的整段文本
 */
fun parseOtpAuthUri(text: String): OtpAuthAccount? {
    val trimmed = text.trim()
    if (!trimmed.startsWith(OTPAUTH_SCHEME, ignoreCase = true)) return null

    val rest = trimmed.substring(OTPAUTH_SCHEME.length)
    val queryIndex = rest.indexOf('?')
    val pathPart = if (queryIndex >= 0) rest.substring(0, queryIndex) else rest
    val queryPart = if (queryIndex >= 0) rest.substring(queryIndex + 1) else ""

    // 形如 totp/服务:账户，前导斜杠可能有多个
    val segments = pathPart.split('/').filter(String::isNotEmpty)
    if (segments.size < 2) return null
    if (!segments.first().equals("totp", ignoreCase = true)) return null

    val label = decodePercent(segments.drop(1).joinToString("/"))
    val params = parseQuery(queryPart)

    // 密钥统一去掉空格并转大写，方便直接使用
    val secret = params["secret"].orEmpty().replace(" ", "").uppercase()
    if (secret.isEmpty()) return null

    // 标签里通常写成「服务:账户」，issuer 参数优先
    val labelIssuer = label.substringBefore(':', missingDelimiterValue = "")
    val labelAccount = label.substringAfter(':', missingDelimiterValue = label)

    val issuer = params["issuer"]?.takeIf(String::isNotBlank) ?: labelIssuer

    return OtpAuthAccount(
        issuer = issuer,
        accountName = labelAccount.ifBlank { issuer },
        secret = secret,
        digits = params["digits"]?.toIntOrNull()?.takeIf { it in SUPPORTED_DIGITS } ?: DEFAULT_DIGITS,
        periodSeconds = params["period"]?.toLongOrNull()?.takeIf { it > 0 } ?: DEFAULT_PERIOD_SECONDS,
        algorithm = params["algorithm"]?.uppercase() ?: DEFAULT_ALGORITHM,
    )
}

/**
 * 解析查询串。
 *
 * 键统一转小写，便于大小写不敏感地取值。
 */
private fun parseQuery(query: String): Map<String, String> {
    if (query.isEmpty()) return emptyMap()

    return query.split('&')
        .filter(String::isNotEmpty)
        .associate { pair ->
            val key = decodePercent(pair.substringBefore('=')).lowercase()
            val value = decodePercent(pair.substringAfter('=', missingDelimiterValue = ""))
            key to value
        }
}

/**
 * 解码 `%XX` 转义。
 *
 * 逐字节还原后再按 UTF-8 解码，这样中文服务名不会乱码。
 */
private fun decodePercent(value: String): String {
    if ('%' !in value) return value

    val bytes = mutableListOf<Byte>()
    var index = 0

    while (index < value.length) {
        val char = value[index]

        // 需要保证 % 后面还有两位十六进制
        if (char == '%' && index + 2 < value.length) {
            val code = value.substring(index + 1, index + 3).toIntOrNull(radix = 16)

            if (code != null) {
                bytes.add(code.toByte())
                index += 3
                continue
            }
        }

        char.toString().encodeToByteArray().forEach(bytes::add)
        index++
    }

    return bytes.toByteArray().decodeToString()
}
