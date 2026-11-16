package com.chow.pomegranate.service.wallpaper

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bing 每日壁纸。
 */
fun Wallpaper(): Wallpaper {
    return WallpaperImpl(
        httpClient = HttpClient {
            defaultRequest {
                url("https://cn.bing.com")
            }
            // 壁纸只是装饰，超时就早点放弃，不要拖住"我的"页
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 8_000
                socketTimeoutMillis = 10_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        },
    )
}

/**
 * Bing 每日壁纸。
 */
interface Wallpaper {
    /**
     * 返回今日壁纸。
     */
    suspend fun getTodayWallpaper(): WallpaperImage
}

/**
 * 壁纸。
 *
 * @property urlString 图片地址
 * @property copyright 版权信息
 */
@Serializable
data class WallpaperImage(
    val urlString: String,
    val copyright: String,
)

/**
 * Bing 壁纸响应。
 */
@Serializable
private data class WallpaperResponse(
    val images: List<WallpaperImageResponse> = emptyList(),
)

/**
 * Bing 壁纸条目。
 */
@Serializable
private data class WallpaperImageResponse(
    val url: String,
    val urlbase: String = "",
    val copyright: String = "",
)

private class WallpaperImpl(
    private val httpClient: HttpClient,
) : Wallpaper {
    override suspend fun getTodayWallpaper(): WallpaperImage {
        val response = httpClient.get("/HPImageArchive.aspx") {
            url {
                parameters.append("format", "js")
                parameters.append("idx", "0")
                parameters.append("n", "1")
                parameters.append("mkt", "zh-CN")
            }
        }.body<WallpaperResponse>()

        val image = response.images.firstOrNull()
            ?: error("无法获取 Bing 每日壁纸。")

        return WallpaperImage(
            // 使用超高清版本
            urlString = "https://cn.bing.com${image.urlbase}_UHD.jpg",
            copyright = image.copyright,
        )
    }
}
