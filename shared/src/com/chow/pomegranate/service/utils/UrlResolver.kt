package com.chow.pomegranate.service.utils

import io.ktor.http.Url

/**
 * 判断 [urlString] 是否完整，若不完整，使用 [lazyRequestUrl] 返回的 [Url] 拼接。
 */
internal inline fun resolveUrl(urlString: String, lazyRequestUrl: () -> Url): String {
    return if ("://" in urlString) {
        urlString
    } else {
        lazyRequestUrl().run { "${protocol.name}://$host/${urlString.removePrefix("/")}" }
    }
}