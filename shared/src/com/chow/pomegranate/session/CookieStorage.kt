package com.chow.pomegranate.session

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

internal const val HOST_ONLY_COOKIE = "pomegranate-host-only"

/** 按 Domain、Path、名称区分 Cookie；快照供安全存储持久化。 */
class CookieStorage(private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : CookiesStorage {
    private val mutex = Mutex()
    private var cookies = mutableListOf<Cookie>()

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cookies.removeAll { it.expires?.timestamp?.let { expiry -> expiry <= now() } == true }
        cookies.filter { cookie ->
            val domain = cookie.domain.orEmpty()
            val host = requestUrl.host.lowercase()
            val hostOnly = cookie.extensions[HOST_ONLY_COOKIE] != "false"
            (host == domain || !hostOnly && host.endsWith(".$domain")) &&
                (!cookie.secure || requestUrl.protocol.name in listOf("https", "wss")) &&
                pathMatches(requestUrl.encodedPath.ifEmpty { "/" }, cookie.path.orEmpty())
        }.sortedByDescending { it.path?.length ?: 0 }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        val hostOnly = cookie.domain.isNullOrBlank()
        val domain = cookie.domain?.trimStart('.')?.lowercase()?.takeIf(String::isNotBlank) ?: requestUrl.host.lowercase()
        if (requestUrl.host.lowercase() != domain && !requestUrl.host.lowercase().endsWith(".$domain")) return
        val defaultPath = requestUrl.encodedPath.substringBeforeLast('/', "").ifEmpty { "/" }
        val path = cookie.path?.takeIf { it.startsWith('/') } ?: defaultPath
        val expires = cookie.maxAge?.let { GMTDate(now() + it.toLong() * 1000) } ?: cookie.expires
        val normalized = cookie.copy(domain = domain, path = path, maxAge = null, expires = expires,
            extensions = cookie.extensions + (HOST_ONLY_COOKIE to hostOnly.toString()))
        mutex.withLock {
            cookies.removeAll { it.name == cookie.name && it.domain == domain && it.path == path }
            if (expires == null || expires.timestamp > now()) cookies.add(normalized)
        }
    }

    override fun close() = Unit
    suspend fun snapshot(): List<Cookie> = mutex.withLock {
        cookies.removeAll { it.expires?.timestamp?.let { expiry -> expiry <= now() } == true }
        cookies.toList()
    }
    suspend fun restore(values: List<Cookie>) = mutex.withLock {
        cookies = values.mapNotNull { cookie ->
            val domain = cookie.domain?.trimStart('.')?.lowercase()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (cookie.expires?.timestamp?.let { it <= now() } == true) return@mapNotNull null
            cookie.copy(domain = domain, path = cookie.path?.takeIf { it.startsWith('/') } ?: "/")
        }.distinctBy { Triple(it.name, it.domain, it.path) }.toMutableList()
    }
    suspend fun clear() = mutex.withLock { cookies.clear() }

    private fun pathMatches(request: String, path: String) = request == path || request.startsWith(path) &&
        (path.endsWith('/') || request.getOrNull(path.length) == '/')
}
