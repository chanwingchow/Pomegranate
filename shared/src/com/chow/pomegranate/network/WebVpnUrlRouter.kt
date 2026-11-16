package com.chow.pomegranate.network

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import io.ktor.http.*
import kotlin.io.encoding.Base64

enum class ConnectionMode { Direct, WebVpn }

/** 与网关公开 ent()/det() 对齐，authority 的默认端口不能省略。 */
class WebVpnUrlRouter(val gatewayAuthority: String = "sec.gdufe.edu.cn:443") {
    val gateway = Url("https://$gatewayAuthority")
    val origin = gateway.toString().trimEnd('/')
    val callback = "https://${gateway.host}/rump_frontend/loginFromCas/"
    private val targets = setOf("jwxt.gdufe.edu.cn", "authserver.gdufe.edu.cn", "2ketang.gdufe.edu.cn")
    private var cachedKey: ByteArray? = null

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun key(): ByteArray = cachedKey ?: CryptographyProvider.Default.get(MD5).hasher()
        .hash(gatewayAuthority.encodeToByteArray()).toHexString().encodeToByteArray().also { cachedKey = it }

    suspend fun encode(value: String): String {
        val key = key()
        val numbers = value.encodeToByteArray().mapIndexed { i, b -> (b.toInt() and 255) + key[i % key.size].toInt() }
        return Base64.UrlSafe.encode(("." + numbers.joinToString(".")).encodeToByteArray())
    }

    private suspend fun decode(value: String): String {
        val key = key()
        return Base64.UrlSafe.decode(value).decodeToString().removePrefix(".").split('.').mapIndexed { i, n ->
            (n.toInt() - key[i % key.size].toInt()).toByte()
        }.toByteArray().decodeToString()
    }

    fun isGateway(url: Url) = url.protocol == URLProtocol.HTTPS && url.host.equals(gateway.host, true) && url.port == gateway.port
    fun isTarget(url: Url) = url.host.lowercase() in targets

    suspend fun route(url: Url, mode: ConnectionMode): Url {
        if (mode == ConnectionMode.Direct || isGateway(url) || url.host.lowercase() !in targets) return url
        require(url.protocol.name in listOf("http", "https")) { "WebVPN 仅支持 HTTP/HTTPS 请求。" }
        return URLBuilder(gateway).apply {
            encodedPath = "/webvpn/${encode(url.protocol.name)}/${encode(url.host + if (url.port != url.protocol.defaultPort) ":${url.port}" else "")}${url.encodedPath.ifEmpty { "/" }}"
            encodedParameters.append("vpn-0", "")
            encodedParameters.appendAll(parseQueryString(url.encodedQuery, decode = false))
            fragment = url.fragment
        }.build()
    }

    suspend fun original(url: Url): Url {
        if (!isGateway(url) || !url.encodedPath.startsWith("/webvpn/")) return url
        val parts = url.encodedPath.removePrefix("/webvpn/").split('/', limit = 3)
        require(parts.size == 3) { "无效的 WebVPN 路由。" }
        return URLBuilder("${decode(parts[0])}://${decode(parts[1])}").apply {
            encodedPath = "/${parts[2]}"
            url.encodedQuery.split('&').filterNot { it.substringBefore('=').startsWith("vpn-") }.filter(String::isNotEmpty)
                .forEach { part -> encodedParameters.append(part.substringBefore('='), part.substringAfter('=', "")) }
            fragment = url.fragment
        }.build()
    }

    suspend fun redirect(current: Url, location: String, mode: ConnectionMode): Url {
        val raw = location.trim()
        require(raw.isNotEmpty() && !raw.contains('\r') && !raw.contains('\n')) { "无效的重定向地址。" }
        val base = if (raw.startsWith("/webvpn/") || raw.startsWith("/rump_frontend/")) gateway else original(current)
        val resolved = URLBuilder(base).apply { takeFrom(raw) }.build()
        require(resolved.protocol.name in listOf("http", "https")) { "不支持的重定向协议。" }
        return route(resolved, mode)
    }
}
