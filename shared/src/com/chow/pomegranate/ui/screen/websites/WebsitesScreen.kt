package com.chow.pomegranate.ui.screen.websites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.link_copied
import com.chow.pomegranate.open_in_browser
import com.chow.pomegranate.useful_websites
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/**
 * 常用网站页导航入口。
 */
fun EntryProviderScope<NavKey>.websitesEntry() {
    entry<WebsitesRoute> {
        WebsitesScreen()
    }
}

/**
 * 常用网站页路由。
 */
@Serializable
data object WebsitesRoute : NavKey

/**
 * 常用网站页。
 */
@Composable
private fun WebsitesScreen(
    modifier: Modifier = Modifier,
) {
    WebsitesContent(
        modifier = modifier,
    )
}

@Preview
@Composable
private fun WebsitesContentPreview() {
    PomegranateExpressiveTheme {
        WebsitesContent()
    }
}

/**
 * 常用网站页内容。
 */
@Composable
private fun WebsitesContent(
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    // 在浏览器打开网站
    val uriHandler = LocalUriHandler.current

    val openInBrowser = stringResource(Res.string.open_in_browser)
    val linkCopied = stringResource(Res.string.link_copied)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLastOrNull()
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(256.dp),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(UsefulWebsite.entries) { website ->
                WebsiteCard(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(website.urlString))
                        scope.launch {
                            snackbarHostState.showSnackbar(linkCopied)
                        }
                    },
                    headlineContent = {
                        Text(stringResource(website.label))
                    },
                    supportingContent = {
                        Text(website.urlString)
                    },
                    leadingContent = {
                        Icon(
                            website.icon,
                            contentDescription = stringResource(website.label),
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { uriHandler.openUri(website.urlString) },
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.OpenInBrowser,
                                contentDescription = openInBrowser,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * 顶部导航。
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PomTopAppBar(
        title = {
            Text(
                stringResource(Res.string.useful_websites),
            )
        },
        modifier = modifier,
        navigationIcon = {
            // 返回按钮
            BackIconButton(
                onClick = onBack,
            )
        },
    )
}

/**
 * 网站卡片。
 */
@Composable
private fun WebsiteCard(
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primaryFixed,
        ),
    ) {
        ListItem(
            headlineContent = {
                // 标题
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    content = headlineContent,
                )
            },
            supportingContent = {
                // 链接
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.outline,
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    content = supportingContent,
                )
            },
            leadingContent = leadingContent,
            trailingContent = {
                // 打开提示
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.primary,
                    LocalTextStyle provides MaterialTheme.typography.labelSmall,
                    content = trailingContent,
                )
            },
        )
    }
}
