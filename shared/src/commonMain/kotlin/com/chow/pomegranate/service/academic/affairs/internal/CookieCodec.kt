package com.chow.pomegranate.service.academic.affairs.internal

import io.ktor.http.Cookie
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter

/** Cookie 字段分隔符。 */
private const val FIELD_SEPARATOR = '\t'

/** Cookie 记录分隔符。 */
private const val RECORD_SEPARATOR = '\n'

/** 布尔字段的假值标记。 */
private const val FALSE_MARKER = "-"

/** 布尔字段的真值标记。 */
private const val TRUE_MARKER = "+"

/** 单条 Cookie 的字段数量。 */
private const val FIELD_COUNT = 6

/**
 * 将 [cookies] 编码为可持久化的字符串。
 *
 * 每条 Cookie 占一行，字段以制表符分隔；域名、路径与 Cookie 值通过 URL 编码转义，
 * 因此可以安全地往返解析。过期时间不参与持久化：会话 Cookie 由服务端续期，
 * 客户端保留过期时间只会让失效的会话更难恢复。
 */
internal fun encodeCookies(cookies: List<Cookie>): String = cookies.joinToString("$RECORD_SEPARATOR") { cookie ->
    listOf(
        cookie.name.encodeURLParameter(),
        cookie.value.encodeURLParameter(),
        cookie.domain.orEmpty().encodeURLParameter(),
        cookie.path.orEmpty().encodeURLParameter(),
        if (cookie.secure) TRUE_MARKER else FALSE_MARKER,
        if (cookie.httpOnly) TRUE_MARKER else FALSE_MARKER,
    ).joinToString("$FIELD_SEPARATOR")
}

/**
 * 将 [encoded] 解析为 Cookie 列表。
 *
 * 无法识别的记录会被跳过，避免单条脏数据导致整个会话无法恢复。
 */
internal fun decodeCookies(encoded: String): List<Cookie> {
    if (encoded.isBlank()) return emptyList()

    return encoded
        .split(RECORD_SEPARATOR)
        .mapNotNull { record ->
            record.split(FIELD_SEPARATOR)
                .takeIf { it.size == FIELD_COUNT }
                ?.toCookie()
        }
}

/** 将单条记录转换为 [Cookie]；字段不合法时返回 `null`。 */
private fun List<String>.toCookie(): Cookie? {
    val name = runCatching { get(0).decodeURLQueryComponent() }.getOrNull() ?: return null
    val value = runCatching { get(1).decodeURLQueryComponent() }.getOrNull() ?: return null

    return Cookie(
        name = name,
        value = value,
        domain = runCatching { get(2).decodeURLQueryComponent() }.getOrDefault("").ifBlank { null },
        path = runCatching { get(3).decodeURLQueryComponent() }.getOrDefault("").ifBlank { null },
        secure = get(4) == TRUE_MARKER,
        httpOnly = get(5) == TRUE_MARKER,
    )
}
