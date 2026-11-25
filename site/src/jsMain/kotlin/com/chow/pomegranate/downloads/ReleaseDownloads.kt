package com.chow.pomegranate.downloads

import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.js.Promise

internal const val PROJECT_URL = "https://github.com/chanwingchow/Pomegranate"
internal const val LATEST_RELEASE_URL = "$PROJECT_URL/releases/latest"
private const val RELEASE_API = "https://api.github.com/repos/chanwingchow/Pomegranate/releases/latest"

internal enum class Platform(val label: String) {
    ANDROID("Android"),
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux"),
    IOS("iOS"),
}

internal enum class PackageType(
    val platform: Platform,
    val suffix: String,
    val label: String,
) {
    ANDROID_ARM64(Platform.ANDROID, "-android-arm64-v8a-release.apk", "ARM64 · .apk"),
    ANDROID_LEGACY(Platform.ANDROID, "-android-release.apk", "安装包 · .apk"),
    WINDOWS_MSI(Platform.WINDOWS, "-windows-x64.msi", "x64 安装包 · .msi"),
    WINDOWS_PORTABLE(Platform.WINDOWS, "-windows-x64-portable.zip", "x64 免安装版 · .zip"),
    MACOS_DMG(Platform.MACOS, "-macos-arm64.dmg", "Apple Silicon · .dmg"),
    LINUX_DEB(Platform.LINUX, "-linux-x64.deb", "Debian / Ubuntu x64 · .deb"),
    IOS_SIMULATOR(Platform.IOS, "-ios-simulator-arm64.zip", "下载模拟器包"),
}

internal data class ReleasePackage(val type: PackageType, val url: String)

internal data class Device(
    val platform: Platform?,
    val architecture: String? = null,
    val bitness: String? = null,
)

internal fun Device.recommendedPackage(packages: List<ReleasePackage>): ReleasePackage? {
    val type = when {
        platform == Platform.ANDROID && architecture == "arm" && bitness == "64" ->
            if (packages.any { it.type == PackageType.ANDROID_ARM64 }) {
                PackageType.ANDROID_ARM64
            } else {
                PackageType.ANDROID_LEGACY
            }
        platform == Platform.ANDROID -> PackageType.ANDROID_LEGACY
        platform == Platform.WINDOWS && architecture == "x86" && bitness == "64" -> PackageType.WINDOWS_MSI
        platform == Platform.MACOS && architecture == "arm" && bitness == "64" -> PackageType.MACOS_DMG
        platform == Platform.LINUX && architecture == "x86" && bitness == "64" -> PackageType.LINUX_DEB
        else -> return null
    }
    return packages.firstOrNull { it.type == type }
}

internal suspend fun detectDevice(): Device {
    val navigator = window.navigator
    val userAgent = navigator.userAgent
    val hints = navigator.asDynamic().userAgentData.unsafeCast<UserAgentHints?>()
    val platform = when {
        userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod") -> Platform.IOS
        userAgent.contains("Macintosh") && navigator.maxTouchPoints > 1 -> Platform.IOS
        hints?.platform == "Android" || userAgent.contains("Android") -> Platform.ANDROID
        hints?.platform == "Chrome OS" || userAgent.contains("CrOS") -> null
        hints?.platform == "Windows" || userAgent.contains("Windows") -> Platform.WINDOWS
        hints?.platform == "macOS" || userAgent.contains("Macintosh") -> Platform.MACOS
        hints?.platform == "Linux" || userAgent.contains("Linux") -> Platform.LINUX
        else -> null
    }
    if (platform == null || platform == Platform.IOS || hints == null) return Device(platform)

    // Only use architecture supplied by the browser. Legacy UA strings can report a default architecture.
    val architecture = try {
        withTimeoutOrNull(1_000) {
            hints.getHighEntropyValues(arrayOf("architecture", "bitness")).await()
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Throwable) {
        null
    }
    return Device(platform, architecture?.architecture, architecture?.bitness)
}

internal suspend fun fetchLatestPackages(): List<ReleasePackage> = try {
    withTimeoutOrNull(5_000) {
        val response = window.fetch(RELEASE_API).await()
        if (!response.ok) return@withTimeoutOrNull emptyList()
        val assets = response.json().await().unsafeCast<GitHubRelease>().assets.orEmpty()
        PackageType.entries.mapNotNull { type ->
            assets.firstNotNullOfOrNull { asset ->
                val url = asset.browser_download_url
                if (asset.name?.endsWith(type.suffix) == true &&
                    url?.startsWith("$PROJECT_URL/releases/download/") == true
                ) {
                    ReleasePackage(type, url)
                } else {
                    null
                }
            }
        }
    }.orEmpty()
} catch (exception: CancellationException) {
    throw exception
} catch (_: Throwable) {
    emptyList()
}

private external interface UserAgentHints {
    val platform: String?
    fun getHighEntropyValues(hints: Array<String>): Promise<ArchitectureHints>
}

private external interface ArchitectureHints {
    val architecture: String?
    val bitness: String?
}

private external interface GitHubRelease {
    val assets: Array<GitHubAsset>?
}

private external interface GitHubAsset {
    val name: String?
    val browser_download_url: String?
}
