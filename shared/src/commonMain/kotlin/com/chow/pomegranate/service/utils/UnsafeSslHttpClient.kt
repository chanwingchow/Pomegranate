package com.chow.pomegranate.service.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * 不安全的 SSL 客户端。
 *
 * 信任所有证书，仅用于证书不规范的目标。
 *
 * @param block 客户端配置
 */
expect fun unsafeSslHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient