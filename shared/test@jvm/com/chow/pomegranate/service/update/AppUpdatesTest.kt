package com.chow.pomegranate.service.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdatesTest {
    @Test
    fun comparesVersionNumbersAndOffersStableReleaseToSameVersionPrerelease() = runTest {
        val service = mockUpdates(release())
        try {
            assertNull(service.latestUpdate("1.10.0", UpdateTarget.AndroidArm64))
            assertNull(service.latestUpdate("1.11.0", UpdateTarget.AndroidArm64))
            assertNotNull(service.latestUpdate("1.9.9", UpdateTarget.AndroidArm64))
            assertNotNull(service.latestUpdate("1.10.0-beta.2", UpdateTarget.AndroidArm64))
        } finally {
            service.close()
        }
    }

    @Test
    fun listsPublishedVersionsAcrossPagesIncludingCurrentAndPrerelease() = runTest {
        val requestedPages = mutableListOf<String>()
        val service = AppUpdates(HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val page = requireNotNull(request.url.parameters["page"])
                    requestedPages += page
                    assertEquals("100", request.url.parameters["per_page"])
                    val body = when (page) {
                        "1" -> listOf(release(), release(tag = "v1.11.0-beta.1", prerelease = true))
                        "2" -> listOf(release(tag = "v1.9.0"), release(tag = "v1.12.0", draft = true))
                        else -> error("Unexpected page $page")
                    }.joinToString(",", "[", "]")
                    val headers = if (page == "1") {
                        headersOf(HttpHeaders.Link, "<$RELEASES_API?page=2>; rel=\"next\"")
                    } else headersOf()
                    respond(body, HttpStatusCode.OK, headers)
                }
            }
        })
        try {
            val releases = service.releases()
            assertEquals(listOf("1", "2"), requestedPages)
            assertEquals(listOf("v1.10.0", "v1.11.0-beta.1", "v1.9.0"), releases.map(AppRelease::tag))
            assertEquals("测试发布", releases.first().notes)
            assertEquals("2026-11-16T14:32:00Z", releases.first().publishedAt)
            assertTrue(releases[1].prerelease)
            assertFalse(releases[2].prerelease)
            assertEquals("$RELEASES_URL/tag/v1.9.0", releases[2].page)
        } finally {
            service.close()
        }
    }

    @Test
    fun onlyOffersMatchingAssetsWithTrustedUrlAndChecksum() = runTest {
        val service = mockUpdates(release(
            asset("android-arm64-v8a-release.apk"),
            asset("android-release.apk"),
            asset("windows-x64.msi"),
            asset("windows-x64-portable.zip"),
            asset("macos-arm64.dmg", root = "https://example.org"),
            asset("linux-x64.deb", digest = ""),
        ))
        try {
            suspend fun names(target: UpdateTarget?) =
                assertNotNull(service.latestUpdate("1.9.0", target)).packages.map(UpdatePackage::name)
            assertEquals(listOf("Pomegranate-1.10.0-android-arm64-v8a-release.apk"), names(UpdateTarget.AndroidArm64))
            assertEquals(listOf("Pomegranate-1.10.0-android-release.apk"), names(UpdateTarget.AndroidOther))
            assertEquals(2, names(UpdateTarget.WindowsX64).size)
            assertTrue(names(UpdateTarget.MacArm64).isEmpty())
            assertTrue(names(UpdateTarget.LinuxX64).isEmpty())
            assertTrue(names(null).isEmpty())
        } finally {
            service.close()
        }
    }

    @Test
    fun doesNotOfferArm64PackageToOtherAndroidDevices() = runTest {
        val service = mockUpdates(release(asset("android-arm64-v8a-release.apk")))
        try {
            val update = assertNotNull(service.latestUpdate("1.9.0", UpdateTarget.AndroidOther))
            assertTrue(update.packages.isEmpty())
        } finally {
            service.close()
        }
    }

    @Test
    fun verifiesContentAndNeverPromotesCorruptOrIncompleteDownloads() = runTest {
        val bytes = "test update".encodeToByteArray()
        val file = "Pomegranate-1.10.0-android-arm64-v8a-release.apk"
        val asset = UpdatePackage(file, "$PROJECT_URL/releases/download/v1.10.0/$file", bytes.size.toLong(), sha256(bytes))
        val temp = Files.createTempDirectory("pomegranate-test-update").toFile()
        var responseBytes = bytes.dropLast(1).toByteArray()
        val service = AppUpdates(HttpClient(MockEngine) { engine { addHandler { respond(responseBytes) } } })
        try {
            val directory = Path(temp.absolutePath)
            val target = temp.resolve(file)
            assertFailsWith<UpdateFailure> { service.download(asset, directory) {} }
            assertFalse(target.exists())
            assertFalse(temp.resolve("$file.part").exists())

            responseBytes = ByteArray(bytes.size)
            assertFailsWith<UpdateFailure> { service.download(asset, directory) {} }
            assertFalse(target.exists())

            responseBytes = bytes
            val path = service.download(asset, directory) {}
            assertTrue(bytes.contentEquals(target.readBytes()))
            service.verify(path, asset)

            target.writeBytes(ByteArray(bytes.size))
            assertFailsWith<UpdateFailure> { service.verify(path, asset) }
            assertEquals(path, service.download(asset, directory) {})
            assertTrue(bytes.contentEquals(target.readBytes()))
        } finally {
            service.close()
            temp.deleteRecursively()
        }
    }

    private fun mockUpdates(body: String) = AppUpdates(HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                assertEquals(UPDATE_API, request.url.toString())
                respond(body, HttpStatusCode.OK)
            }
        }
    })

    private fun release(vararg assets: String, tag: String = "v1.10.0", prerelease: Boolean = false, draft: Boolean = false) =
        """{"tag_name":"$tag","body":"测试发布","published_at":"2026-11-16T14:32:00Z","prerelease":$prerelease,"draft":$draft,"assets":[${assets.joinToString(",")}]}"""

    private fun asset(
        suffix: String,
        root: String = PROJECT_URL,
        digest: String = "sha256:${"a".repeat(64)}",
    ): String {
        val name = "Pomegranate-1.10.0-$suffix"
        return """{"name":"$name","size":11,"digest":"$digest","browser_download_url":"$root/releases/download/v1.10.0/$name"}"""
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
}
