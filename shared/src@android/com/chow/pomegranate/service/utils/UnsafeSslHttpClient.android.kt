package com.chow.pomegranate.service.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual fun unsafeSslHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(block)
}