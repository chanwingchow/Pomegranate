package com.chow.pomegranate.session

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存 Cookie 仓库。
 *
 * 与 Ktor 的 `AcceptAllCookiesStorage` 等价，但对外暴露全部 Cookie，
 * 以便会话管理器把 Cookie 持久化到本地、或在重新登录前恢复。
 */
class CookieStorage : CookiesStorage {
    /** Cookie 集合，键为请求 host。 */
    private var cookies: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    /** 互斥锁。 */
    private val mutex = Mutex()

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cookies[requestUrl.host]?.toList() ?: emptyList()
    }

    override suspend fun addCookie(
        requestUrl: Url,
        cookie: Cookie,
    ) {
        // 服务端未下发 Domain 时补上请求 host：
        // Ktor 的 Cookie.matches 要求 cookie.domain 与请求 host 匹配，
        // 缺少 Domain 的会话 Cookie 会被判定为不匹配，永远发不回去。
        val normalized = if (cookie.domain.isNullOrBlank()) {
            cookie.copy(domain = requestUrl.host)
        } else {
            cookie
        }

        mutex.withLock {
            cookies.getOrPut(requestUrl.host) { mutableListOf() }.apply {
                // 同名 Cookie 只保留最新的一条
                removeAll { it.name == normalized.name }
                add(normalized)
            }
        }
    }

    override fun close() = Unit

    /** 返回当前全部 Cookie。 */
    suspend fun snapshot(): List<Cookie> = mutex.withLock {
        cookies.values.flatten()
    }

    /** 用 [cookies] 替换当前全部 Cookie。 */
    suspend fun restore(cookies: List<Cookie>) {
        mutex.withLock {
            val restored = mutableMapOf<String, MutableList<Cookie>>()

            // 按 Domain 归档，与 get() 用的 requestUrl.host 对齐。
            // 早先版本用 domain.orEmpty()，无 Domain 的会话 Cookie 全落在空键下，
            // 按 host 查不到，导致保存的会话永远恢复不了、自动登录形同虚设。
            cookies.forEach { cookie ->
                val host = cookie.domain?.takeIf { it.isNotBlank() } ?: return@forEach

                restored.getOrPut(host) { mutableListOf() }.apply {
                    removeAll { it.name == cookie.name }
                    add(cookie)
                }
            }

            this.cookies = restored
        }
    }

    /** 清空全部 Cookie。 */
    suspend fun clear() {
        mutex.withLock { cookies.clear() }
    }
}
