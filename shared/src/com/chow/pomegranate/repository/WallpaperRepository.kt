package com.chow.pomegranate.repository

import com.chow.pomegranate.utils.runSuspendCatching
import com.chow.pomegranate.service.wallpaper.Wallpaper
import com.chow.pomegranate.service.wallpaper.WallpaperImage
import org.koin.core.annotation.Single

/** 壁纸数据仓库。 */
@Single
class WallpaperRepository(
    private val wallpaper: Wallpaper,
) {
    /** 缓存，避免重复请求。 */
    private var cached: WallpaperImage? = null

    /**
     * 返回今日壁纸；请求失败时返回 `null`，由调用方回退到默认背景。
     */
    suspend fun getTodayWallpaper(): WallpaperImage? {
        cached?.let { return it }

        return runSuspendCatching { wallpaper.getTodayWallpaper() }
            .onSuccess { cached = it }
            .getOrNull()
    }
}
