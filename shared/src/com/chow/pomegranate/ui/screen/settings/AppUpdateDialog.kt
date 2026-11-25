package com.chow.pomegranate.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.service.update.AppRelease
import com.chow.pomegranate.service.update.PROJECT_URL
import com.chow.pomegranate.service.update.UpdateInstaller
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.time.Instant

@Serializable
internal data class AppUpdateRoute(val automatic: Boolean = false) : NavKey

@Composable
internal fun AppUpdateDialog(route: AppUpdateRoute, installer: UpdateInstaller?) {
    val viewModel = LocalAppUpdateViewModel.current ?: return
    val backStack = LocalBackStack.current
    val dialogState by viewModel.state.collectAsState()
    val latest by viewModel.latest.collectAsState()
    val latestResolved by viewModel.latestResolved.collectAsState()
    val dismiss: () -> Unit = {
        if (backStack.lastOrNull() == route && viewModel.dismissUpdate()) backStack.removeLastOrNull()
    }
    LaunchedEffect(latest, latestResolved) {
        if (latest != null) viewModel.openUpdate()
        else if (latestResolved) dismiss()
    }
    val state = dialogState
    if (state == null) {
        if (!latestResolved) {
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("下载更新") },
                text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = dismiss) { Text("关闭") } },
            )
        }
        return
    }
    val uriHandler = LocalUriHandler.current
    var browserError by remember { mutableStateOf(false) }
    val openRelease: (String) -> Unit = { url ->
        browserError = runCatching { uriHandler.openUri(url) }.isFailure
    }
    val automaticPrompt = route.automatic && state is AppUpdateState.Available
    val postpone: () -> Unit = { viewModel.postponeStartupPrompt(skipVersion = false, onSaved = dismiss) }

    AlertDialog(
        onDismissRequest = if (automaticPrompt) postpone else dismiss,
        title = {
            Text(when (state) {
                is AppUpdateState.Available -> if (automaticPrompt) "发现新版本" else "下载更新"
                is AppUpdateState.Downloading -> "正在下载更新"
                is AppUpdateState.Ready -> "更新已下载"
            })
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (state) {
                    is AppUpdateState.Available -> {
                        val release = state.update.release
                        Text("当前版本 ${BuildConfig.versionName}")
                        Text("新版本 ${release.version}", style = MaterialTheme.typography.titleMedium)
                        ReleaseNotes(release.notes, openRelease)
                        if (state.update.packages.isEmpty()) {
                            Text("当前设备请前往发布页获取更新。")
                        }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (installer != null) {
                            val portable = state.update.packages.drop(1).firstOrNull { it.portable }
                            if (portable != null) {
                                TextButton(onClick = { viewModel.download(portable, installer) }) {
                                    Text("下载 Windows 免安装版")
                                }
                            }
                        }
                        if (state.update.packages.isNotEmpty()) {
                            TextButton(onClick = { openRelease(release.page) }) { Text("查看发布页") }
                        }
                    }
                    is AppUpdateState.Downloading -> {
                        Text("版本 ${state.update.release.version}")
                        Text("已下载 ${(state.progress * 100).roundToInt()}%")
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.Ready -> {
                        Text("版本 ${state.update.release.version}")
                        Text(state.asset.name)
                        state.message?.let { Text(it) }
                        if (state.opening) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { openRelease(state.update.release.page) }) { Text("查看发布页") }
                    }
                }
                if (browserError) Text("无法打开浏览器。", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            when (state) {
                is AppUpdateState.Available -> {
                    val asset = state.update.packages.firstOrNull()
                    if (asset != null && installer != null) {
                        TextButton(onClick = { viewModel.download(asset, installer) }) {
                            Text(if (automaticPrompt) "下载更新" else "下载")
                        }
                    } else {
                        TextButton(onClick = { openRelease(state.update.release.page) }) { Text("查看发布页") }
                    }
                }
                is AppUpdateState.Downloading -> {
                    TextButton(onClick = viewModel::cancelDownload) { Text("取消下载") }
                }
                is AppUpdateState.Ready -> if (installer != null) {
                    TextButton(enabled = !state.opening, onClick = { viewModel.openInstaller(installer) }) {
                        Text(if (state.asset.portable) "打开免安装包" else installer.openLabel)
                    }
                }
            }
        },
        dismissButton = {
            if (automaticPrompt) {
                FlowRow(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.postponeStartupPrompt(skipVersion = true, onSaved = dismiss) }) {
                        Text("不再提醒此版本")
                    }
                    TextButton(onClick = postpone) { Text("稍后") }
                }
            } else if (state !is AppUpdateState.Downloading) {
                val opening = (state as? AppUpdateState.Ready)?.opening == true
                TextButton(enabled = !opening, onClick = dismiss) { Text("关闭") }
            }
        },
    )
}

internal fun AppRelease.publishedDate(): String? = publishedAt?.let { timestamp ->
    runCatching { Instant.parse(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() }.getOrNull()
}

@Composable
internal fun ReleaseNotes(notes: String, openRelease: (String) -> Unit) {
    if (notes.isBlank()) {
        Text("这个版本没有发布说明。")
        return
    }

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    val lines = remember(notes) {
        notes.lineSequence().map(String::trim).filter {
            it.isNotEmpty() && it != "<details>" && it != "</details>"
        }.toList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in lines) {
            val heading = RELEASE_HEADING.matchEntire(line)?.groupValues?.get(1)
                ?: RELEASE_SUMMARY.matchEntire(line)?.groupValues?.get(1)
            if (heading != null) {
                Text(heading, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall)
            } else {
                val bullet = line.startsWith("- ") || line.startsWith("* ")
                val content = if (bullet) line.drop(2) else line
                Text(buildAnnotatedString {
                    if (bullet) append("• ")
                    var cursor = 0
                    for (link in RELEASE_LINK.findAll(content)) {
                        append(content.substring(cursor, link.range.first).plainReleaseText())
                        val (label, url) = link.destructured
                        if (url.startsWith("$PROJECT_URL/")) {
                            withLink(LinkAnnotation.Clickable(url, linkStyles, linkInteractionListener = { openRelease(url) })) {
                                append(label)
                            }
                        } else {
                            append(label)
                        }
                        cursor = link.range.last + 1
                    }
                    append(content.substring(cursor).plainReleaseText())
                })
            }
        }
    }
}

private val RELEASE_HEADING = Regex("""#{1,6}\s+(.+)""")
private val RELEASE_SUMMARY = Regex("""<summary>(.+)</summary>""")
private val RELEASE_LINK = Regex("""\[([^\]]+)]\((https://[^\s)]+)\)""")

private fun String.plainReleaseText(): String = replace("\u0060", "").replace("**", "")
