package com.chow.pomegranate.components.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chow.pomegranate.downloads.Device
import com.chow.pomegranate.downloads.LATEST_RELEASE_URL
import com.chow.pomegranate.downloads.Platform
import com.chow.pomegranate.downloads.ReleasePackage
import com.chow.pomegranate.downloads.detectDevice
import com.chow.pomegranate.downloads.fetchLatestPackages
import com.chow.pomegranate.downloads.recommendedPackage
import com.varabyte.kobweb.compose.dom.GenericTag
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import kotlinx.coroutines.async
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.Element

@Composable
internal fun Downloads() {
    var device by remember { mutableStateOf(Device(null)) }
    var packages by remember { mutableStateOf(emptyList<ReleasePackage>()) }
    if (!AppGlobals.isExporting) {
        LaunchedEffect(Unit) {
            val deviceResult = async { detectDevice() }
            packages = fetchLatestPackages()
            device = deviceResult.await()
        }
    }

    val recommended = device.recommendedPackage(packages)
    Section(attrs = {
        classes("downloads")
        attr("aria-label", "下载小石榴")
        if (recommended != null) attr("data-recommended", "")
    }) {
        if (recommended != null) {
            A(href = recommended.url, attrs = { classes("download-primary") }) {
                Text("下载 ${recommended.type.platform.label} 版")
            }
            P(attrs = { classes("download-context") }) { Text(recommended.type.label) }
        } else if (device.platform == Platform.IOS) {
            P(attrs = { classes("download-context") }) { Text("iPhone 和 iPad 暂无安装包。") }
        }

        GenericTag<Element>("details", attrs = { classes("download-options") }) {
            GenericTag<Element>("summary") {
                Text(if (recommended == null) "查看可用版本" else "查看其他版本")
            }
            VersionList(packages.filter { it != recommended })
        }
    }
}

@Composable
private fun VersionList(packages: List<ReleasePackage>) {
    Div(attrs = { classes("versions-panel") }) {
        if (packages.isEmpty()) {
            P(attrs = { classes("versions-note") }) {
                A(href = LATEST_RELEASE_URL) { Text("在发布页查看安装包") }
            }
            return@Div
        }

        val byPlatform = packages.groupBy { it.type.platform }
        val platforms = byPlatform.keys.filter { it != Platform.IOS }
        if (platforms.isNotEmpty()) {
            Div(attrs = { classes("platforms") }) {
                platforms.forEach { platform ->
                    Div(attrs = { classes("platform") }) {
                        H2 {
                            Span(attrs = {
                                classes("platform-icon", "icon-${platform.name.lowercase()}")
                                attr("aria-hidden", "true")
                            })
                            Text(platform.label)
                        }
                        byPlatform.getValue(platform).forEach { release ->
                            A(href = release.url, attrs = { classes("release-link") }) {
                                Text(release.type.label)
                            }
                        }
                    }
                }
            }
        }
        byPlatform[Platform.IOS]?.firstOrNull()?.let { simulator ->
            P(attrs = { classes("versions-note") }) {
                Text("iOS 仅提供模拟器包。 ")
                A(href = simulator.url) { Text(simulator.type.label) }
            }
        }
    }
}
