package com.chow.pomegranate.service.update

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val PROJECT_URL = "https://github.com/chanwingchow/Pomegranate"
internal const val RELEASES_URL = "$PROJECT_URL/releases"
internal const val RELEASES_API = "https://api.github.com/repos/chanwingchow/Pomegranate/releases"
internal const val UPDATE_API = "$RELEASES_API/latest"
private const val MAX_PACKAGE_SIZE = 512L * 1024 * 1024
private val SHA256_DIGEST = Regex("sha256:([a-fA-F0-9]{64})")
private val downloadLock = Mutex()

internal enum class UpdateTarget(val packageSuffixes: List<String>) {
    AndroidArm64(listOf("android-arm64-v8a-release.apk", "android-release.apk")),
    AndroidOther(listOf("android-release.apk")),
    WindowsX64(listOf("windows-x64.msi", "windows-x64-portable.zip")),
    MacArm64(listOf("macos-arm64.dmg")),
    LinuxX64(listOf("linux-x64.deb")),
}

internal data class UpdatePackage(val name: String, val url: String, val bytes: Long, val sha256: String) {
    val portable: Boolean get() = name.endsWith("-portable.zip")
}

internal data class AppRelease(
    val tag: String,
    val notes: String,
    val publishedAt: String?,
    val prerelease: Boolean,
) {
    val version: String get() = tag.removePrefix("v")
    val page: String get() = "$RELEASES_URL/tag/${tag.encodeURLPathPart()}"
}

internal data class AppUpdate(val release: AppRelease, val packages: List<UpdatePackage>)

internal class UpdateFailure(message: String) : Exception(message)

internal class AppUpdates(
    private val client: HttpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
    },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val files = SystemFileSystem
    private val sha256 by lazy { CryptographyProvider.Default.get(SHA256).hasher() }

    suspend fun latestUpdate(currentVersion: String, target: UpdateTarget?): AppUpdate? {
        val current = AppVersion.parse(currentVersion)
            ?: throw UpdateFailure("无法识别当前应用版本。")
        val response = client.get(UPDATE_API) {
            accept(ContentType.parse("application/vnd.github+json"))
            header(HttpHeaders.CacheControl, "no-cache")
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (response.status != HttpStatusCode.OK) {
            throw UpdateFailure("暂时无法读取发布信息，请稍后重试。")
        }

        val release = json.decodeFromString<GitHubRelease>(response.bodyAsText())
        val version = AppVersion.parse(release.tag)
        if (version == null || version.prerelease || release.prerelease || release.draft) {
            throw UpdateFailure("暂时无法读取正式版信息。")
        }
        if (version <= current) return null

        val versionName = release.tag.removePrefix("v")
        val assetRoot = "$PROJECT_URL/releases/download/${release.tag}"
        val packages = target?.packageSuffixes.orEmpty().mapNotNull { suffix ->
            val name = "Pomegranate-$versionName-$suffix"
            val asset = release.assets.firstOrNull {
                it.name == name && it.url == "$assetRoot/$name" && it.size in 1..MAX_PACKAGE_SIZE
            } ?: return@mapNotNull null
            val digest = asset.digest?.let(SHA256_DIGEST::matchEntire)?.groupValues?.get(1)
                ?: return@mapNotNull null
            UpdatePackage(name, asset.url, asset.size, digest.lowercase())
        }
        // Prefer the new ABI-specific name while still recognizing existing releases.
        val compatible = if (target == UpdateTarget.AndroidArm64) packages.take(1) else packages
        return AppUpdate(release.toAppRelease(), compatible)
    }

    suspend fun releases(): List<AppRelease> {
        val releases = mutableListOf<AppRelease>()
        var page = 1
        do {
            val response = client.get(RELEASES_API) {
                accept(ContentType.parse("application/vnd.github+json"))
                parameter("per_page", 100)
                parameter("page", page++)
            }
            if (response.status != HttpStatusCode.OK) {
                throw UpdateFailure("暂时无法读取版本列表，请稍后重试。")
            }
            json.decodeFromString<List<GitHubRelease>>(response.bodyAsText())
                .filterNot { it.draft || it.tag.isBlank() }
                .mapTo(releases) { it.toAppRelease() }
        } while (response.headers[HttpHeaders.Link]?.contains("rel=\"next\"") == true)
        return releases.distinctBy(AppRelease::tag).sortedByDescending(AppRelease::publishedAt)
    }

    suspend fun download(asset: UpdatePackage, directory: Path, onProgress: (Float) -> Unit): Path =
        downloadLock.withLock {
            withContext(Dispatchers.IO) {
                files.createDirectories(directory)
                val target = Path(directory, asset.name)
                if (matches(target, asset)) {
                    onProgress(1f)
                    return@withContext target
                }

                val partial = Path(directory, "${asset.name}.part")
                try {
                    client.prepareGet(asset.url) {
                        timeout { requestTimeoutMillis = 10 * 60_000 }
                    }.execute { response ->
                        if (response.status != HttpStatusCode.OK) {
                            throw UpdateFailure("安装包下载失败，请稍后重试。")
                        }
                        val input = response.bodyAsChannel()
                        val buffer = ByteArray(64 * 1024)
                        var received = 0L
                        var lastPercent = -1
                        sha256.createHashFunction().use { hash ->
                            files.sink(partial).buffered().use { output ->
                                while (true) {
                                    val read = input.readAvailable(buffer)
                                    if (read < 0) break
                                    received += read
                                    if (received > asset.bytes) throw invalidPackage()
                                    output.write(buffer, 0, read)
                                    hash.update(buffer, 0, read)
                                    val percent = (100 * received / asset.bytes).toInt()
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        onProgress(percent / 100f)
                                    }
                                }
                            }
                            if (received != asset.bytes || hash.hashToByteArray().toHexString() != asset.sha256) {
                                throw invalidPackage()
                            }
                        }
                    }
                    files.atomicMove(partial, target)
                    target
                } finally {
                    files.delete(partial, mustExist = false)
                }
            }
        }

    suspend fun verify(path: Path, asset: UpdatePackage) = withContext(Dispatchers.IO) {
        if (!matches(path, asset)) throw invalidPackage()
    }

    private fun matches(path: Path, asset: UpdatePackage): Boolean {
        val metadata = files.metadataOrNull(path) ?: return false
        if (!metadata.isRegularFile || metadata.size != asset.bytes) return false
        return sha256.createHashFunction().use { hash ->
            files.source(path).use(hash::update)
            hash.hashToByteArray().toHexString() == asset.sha256
        }
    }

    private fun invalidPackage() = UpdateFailure("安装包校验失败，请重新下载。")

    fun close() = client.close()
}

private val VERSION_PATTERN = Regex("""v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?""")

private data class AppVersion(val major: Int, val minor: Int, val patch: Int, val prerelease: Boolean) :
    Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this, other, AppVersion::major, AppVersion::minor, AppVersion::patch, { !it.prerelease },
    )

    companion object {
        fun parse(value: String): AppVersion? {
            val parts = VERSION_PATTERN.matchEntire(value)?.groupValues ?: return null
            return AppVersion(
                parts[1].toIntOrNull() ?: return null,
                parts[2].toIntOrNull() ?: return null,
                parts[3].toIntOrNull() ?: return null,
                parts[4].isNotEmpty(),
            )
        }
    }
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tag: String,
    @SerialName("body") val notes: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
) {
    fun toAppRelease() = AppRelease(
        tag, notes.orEmpty().trim(), publishedAt, prerelease || AppVersion.parse(tag)?.prerelease == true,
    )
}

@Serializable
private data class GitHubAsset(
    val name: String,
    val size: Long,
    val digest: String? = null,
    @SerialName("browser_download_url") val url: String,
)
