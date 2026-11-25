package com.chow.pomegranate.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.service.update.AppRelease
import com.chow.pomegranate.service.update.RELEASES_URL
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.serialization.Serializable

internal val LocalAppUpdateViewModel = staticCompositionLocalOf<AppUpdateViewModel?> { null }

@Serializable
internal data object VersionHistoryRoute : NavKey

@Serializable
internal data class VersionDetailsRoute(val tag: String) : NavKey

@Composable
internal fun VersionHistoryScreen() {
    val viewModel = LocalAppUpdateViewModel.current ?: return
    val state by viewModel.history.collectAsState()
    val latest by viewModel.latest.collectAsState()
    val backStack = LocalBackStack.current
    LaunchedEffect(viewModel) { viewModel.loadHistory() }

    VersionPage("版本列表") {
        when (val history = state) {
            ReleaseHistoryState.Loading -> HistoryLoading()
            is ReleaseHistoryState.Failed -> HistoryError(history.message, viewModel::loadHistory)
            is ReleaseHistoryState.Loaded -> LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "当前安装版本 ${BuildConfig.versionName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                if (history.releases.isEmpty()) {
                    item { Text("暂无公开发布的版本。") }
                }
                items(history.releases, key = AppRelease::tag) { release ->
                    VersionRow(release, isNew = latest?.release?.tag == release.tag) {
                        backStack.add(VersionDetailsRoute(release.tag))
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(release: AppRelease, isNew: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(release.tag, style = MaterialTheme.typography.titleMedium)
                    val label = when {
                        release.version == BuildConfig.versionName.removePrefix("v") -> "当前安装"
                        isNew -> "可更新"
                        release.prerelease -> "预发布"
                        else -> null
                    }
                    label?.let {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) { Text(it, modifier = Modifier.padding(horizontal = 4.dp)) }
                    }
                }
                release.publishedDate()?.let { date ->
                    Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
internal fun VersionDetailsScreen(tag: String) {
    val viewModel = LocalAppUpdateViewModel.current ?: return
    val state by viewModel.history.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadHistory() }

    VersionPage("版本详情") {
        when (val history = state) {
            ReleaseHistoryState.Loading -> HistoryLoading()
            is ReleaseHistoryState.Failed -> HistoryError(history.message, viewModel::loadHistory)
            is ReleaseHistoryState.Loaded -> {
                val release = history.releases.firstOrNull { it.tag == tag }
                if (release != null) VersionDetails(release)
                else Text("此版本的发布说明暂不可用。", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun VersionDetails(release: AppRelease) {
    val uriHandler = LocalUriHandler.current
    var browserError by remember(release.tag) { mutableStateOf(false) }
    val openRelease: (String) -> Unit = { url ->
        browserError = runCatching { uriHandler.openUri(url) }.isFailure
    }
    Column(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(release.tag, style = MaterialTheme.typography.headlineMedium)
        release.publishedDate()?.let { date ->
            Text("发布于 $date", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ReleaseNotes(release.notes, openRelease)
        TextButton(onClick = { openRelease(release.page) }) { Text("查看发布页") }
        if (browserError) Text("无法打开浏览器。", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun VersionPage(title: String, content: @Composable () -> Unit) {
    val backStack = LocalBackStack.current
    Scaffold(topBar = {
        PomTopAppBar(
            title = { Text(title) },
            navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
        )
    }) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) { content() }
    }
}

@Composable
private fun HistoryLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HistoryError(message: String, retry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = retry) { Text("重试") }
            TextButton(onClick = { runCatching { uriHandler.openUri(RELEASES_URL) } }) { Text("查看 GitHub 发布页") }
        }
    }
}
