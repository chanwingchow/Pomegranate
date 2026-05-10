package com.chow.pomegranate.ui.screen.websites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.useful_websites

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
data object WebsitesRoute: NavKey

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLast()
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
                    onClick = {},
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
                Box(
                    modifier = Modifier.padding(end = 48.dp),
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        content = headlineContent,
                    )
                }
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
        )
    }
}