package com.chow.pomegranate.ui.screen.websites

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.chow.pomegranate.ui.component.BackToTopButton
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.tool.clipboard.writePlainText
import com.chow.pomegranate.clipboard_copy_failed
import com.chow.pomegranate.Res
import com.chow.pomegranate.link_copied
import com.chow.pomegranate.open_link_failed
import com.chow.pomegranate.useful_websites
import com.chow.pomegranate.website_copy
import com.chow.pomegranate.website_open
import com.chow.pomegranate.websites_campus_services
import com.chow.pomegranate.websites_learning
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ExpressiveActionCard
import com.chow.pomegranate.ui.component.ExpressiveIconBadge
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

fun EntryProviderScope<NavKey>.websitesEntry() {
    entry<WebsitesRoute> { WebsitesContent() }
}

@Serializable
data object WebsitesRoute : NavKey

@Preview(name = "手机", widthDp = 411, heightDp = 800)
@Preview(name = "窄屏", widthDp = 320, heightDp = 640)
@Preview(name = "宽屏", widthDp = 840, heightDp = 700)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WebsitesContentPreview() {
    DynamicMaterialExpressiveTheme(state = rememberDynamicMaterialThemeState(primary = DEFAULT_PRIMARY_COLOR, isDark = false)) {
        CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }) { WebsitesContent() }
    }
}

/** 网址按用途分组；复制与外部浏览器操作分别有清楚的按钮。 */
@Composable
private fun WebsitesContent(modifier: Modifier = Modifier) {
    val backStack = LocalBackStack.current
    val gridState = rememberLazyGridState()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val copyFailedMessage = stringResource(Res.string.clipboard_copy_failed)
    val copiedMessage = stringResource(Res.string.link_copied)
    val openFailed = stringResource(Res.string.open_link_failed)
    val learning = listOf(UsefulWebsite.Library, UsefulWebsite.AcademicAffairsSystem, UsefulWebsite.ExtracurricularActivities,
        UsefulWebsite.GdufeMooc, UsefulWebsite.GdufeBlackboard)
    val campusServices = UsefulWebsite.entries.filterNot { it in learning }
    val groups = listOf(Res.string.websites_learning to learning, Res.string.websites_campus_services to campusServices)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = { Text(stringResource(Res.string.useful_websites)) },
                navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
            )
        },
        floatingActionButton = { BackToTopButton(gridState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp * fontScale), state = gridState,
                modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                groups.forEach { (title, websites) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(websites, key = { it.name }) { website ->
                        WebsiteCard(
                            website = website,
                            onCopy = {
                                val copied = try {
                                    clipboard.writePlainText(website.urlString)
                                    true
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { false }
                                scope.launch {
                                    val feedback = if (copied) copiedMessage else copyFailedMessage
                                    snackbar.currentSnackbarData?.takeIf { it.visuals.message == feedback }?.dismiss()
                                    snackbar.showSnackbar(feedback)
                                }
                                copied
                            },
                            onOpen = {
                                runCatching { uriHandler.openUri(website.urlString) }
                                    .onFailure { scope.launch { snackbar.showSnackbar(openFailed) } }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsiteCard(website: UsefulWebsite, onCopy: suspend () -> Boolean, onOpen: () -> Unit) {
    val label = stringResource(website.label)
    val copyLabel = stringResource(Res.string.website_copy, label)
    val openLabel = stringResource(Res.string.website_open, label)
    val domain = remember(website) { website.urlString.substringAfter("://").substringBefore('/') }
    var copyCount by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val copy: () -> Unit = { scope.launch { if (onCopy()) copyCount++ } }
    LaunchedEffect(copyCount) {
        if (copyCount > 0) { copied = true; delay(2_000); copied = false }
    }
    ExpressiveActionCard(onClick = onOpen, modifier = Modifier.semantics { onClick(label = openLabel, action = null) }) { pressed ->
        BoxWithConstraints(Modifier.padding(14.dp)) {
            val stackActions = LocalDensity.current.fontScale > 1.3f || maxWidth < 240.dp
            val actions: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = copy, modifier = Modifier.size(40.dp)) {
                        Crossfade(targetState = copied, label = "Link copied") { isCopied ->
                            Icon(if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                copyLabel, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    FilledTonalIconButton(onClick = onOpen, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, openLabel, Modifier.size(20.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpressiveIconBadge(website.icon, website.ordinal, pressed)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(domain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (stackActions) 2 else 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (!stackActions) actions()
                }
                if (stackActions) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
            }
        }
    }
}
