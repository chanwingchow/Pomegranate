package com.chow.pomegranate.service.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun unsafeSslHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    val trustManager = @Suppress("CustomX509TrustManager")
    object : X509TrustManager {
        @Suppress("TrustAllX509TrustManager")
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        @Suppress("TrustAllX509TrustManager")
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    }

    return HttpClient(OkHttp) {
        engine {
            config {
                sslSocketFactory(sslContext.socketFactory, trustManager)
            }
        }
        block()
    }
}