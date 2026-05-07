package com.chow.pomegranate.ui.screen.notice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.academic_notice
import pomegranate.composeapp.generated.resources.add
import pomegranate.composeapp.generated.resources.app_name

/**
 * 教务通知页导航入口。
 */
fun EntryProviderScope<NavKey>.noticeEntry() {
    entry<NoticeRoute> {
        NoticeScreen()
    }
}

/**
 * 教务通知页路由。
 */
@Serializable
data object NoticeRoute : NavKey

/**
 * 教务通知页。
 */
@Composable
private fun NoticeScreen(
    modifier: Modifier = Modifier,
) {
    NoticeContent(
        modifier = modifier,
    )
}

@Preview
@Composable
private fun NoticeContentPreview() {
    PomegranateExpressiveTheme {
        NoticeContent()
    }
}

@Composable
private fun NoticeContent(
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current

    val scope = rememberCoroutineScope()

    val lazyStaggeredGridState = rememberLazyStaggeredGridState()

    // 高斯模糊
    val sky = rememberSky()

    // 悬浮按钮是否可见
    var floatingActionButtonVisible by remember { mutableStateOf(true) }

    // 滚动到顶部按钮是否可见
    var scrollToTopButtonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 监听列表第一个可见项下标
        snapshotFlow { lazyStaggeredGridState.firstVisibleItemIndex }
            .collect { index ->
                // 在第一个可见下标不为 0 时，展示滚动到顶部按钮
                scrollToTopButtonVisible = index > 0
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLast()
                },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                floatingActionButtonVisible,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
            ) {
                // 滚动到顶部按钮
                AnimatedVisibility(
                    scrollToTopButtonVisible,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut(),
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                // 滚动到顶部
                                lazyStaggeredGridState.animateScrollToItem(0)
                            }
                        },
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = stringResource(Res.string.add),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            modifier = Modifier.sky(sky = sky),
            state = lazyStaggeredGridState,
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(10) {
                NoticeCard(
                    onClick = {},
                    title = {
                        Text(
                            stringResource(Res.string.app_name),
                        )
                    },
                    date = {
                        Text("2026-5-7")
                    },
                    url = {
                        Text("https://example.com")
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
                stringResource(Res.string.academic_notice),
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
 * 通知卡片。
 */
@Composable
private fun NoticeCard(
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    date: @Composable () -> Unit,
    url: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box {
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
                            content = title,
                        )
                    }
                },
                supportingContent = {
                    // 链接
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                        LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    ) {
                        url()
                    }
                },
            )
        }

        // 日期
        Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.labelSmall,
                ) {
                    date()
                }
            }
        }
    }
}