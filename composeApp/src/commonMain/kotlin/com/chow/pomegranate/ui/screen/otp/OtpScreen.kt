package com.chow.pomegranate.ui.screen.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
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
import pomegranate.composeapp.generated.resources.add
import pomegranate.composeapp.generated.resources.delete
import pomegranate.composeapp.generated.resources.edit
import pomegranate.composeapp.generated.resources.otp

/**
 * OTP 页导航入口。
 */
fun EntryProviderScope<NavKey>.otpEntry() {
    entry<OtpRoute> {
        OtpScreen()
    }
}

/**
 * OTP 页路由。
 */
@Serializable
data object OtpRoute : NavKey

/**
 * OTP 页。
 */
@Composable
private fun OtpScreen(
    modifier: Modifier = Modifier,
) {
    OtpContent(
        modifier = modifier,
    )
}

@Preview
@Composable
private fun OtpContentPreview() {
    PomegranateExpressiveTheme {
        OtpContent()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtpContent(
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current

    val scope = rememberCoroutineScope()

    val lazyGridState = rememberLazyGridState()

    // 高斯模糊
    val sky = rememberSky()

    // 悬浮按钮是否可见
    var floatingActionButtonVisible by remember { mutableStateOf(true) }

    // 滚动到顶部按钮是否可见
    var scrollToTopButtonVisible by remember { mutableStateOf(false) }

    // 是否正在编辑
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 监听列表第一个可见项下标
        snapshotFlow { lazyGridState.firstVisibleItemIndex }
            .collect { index ->
                // 在第一个可见下标不为 0 时，展示滚动到顶部按钮
                scrollToTopButtonVisible = index > 0
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                isEditing = isEditing,
                onEditingChange = { isEditing = it },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    lazyGridState.animateScrollToItem(0)
                                }
                            },
                        ) {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = stringResource(Res.string.add),
                            )
                        }
                    }

                    // 添加按钮
                    SmallFloatingActionButton(
                        onClick = {},
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(Res.string.add),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.floatingToolbarVerticalNestedScroll(
                expanded = floatingActionButtonVisible,
                onExpand = { floatingActionButtonVisible = true },
                onCollapse = { floatingActionButtonVisible = false },
            ).sky(sky = sky),
            state = lazyGridState,
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(300) { item ->
                OtpCard(
                    onClick = {},
                    headlineContent = {
                        // 名称
                        Text("OTP $item")
                    },
                    supportingContent = {
                        // 密码
                        Text(
                            "066 100",
                        )
                    },
                    trailingContent = {
                        AnimatedVisibility(
                            isEditing,
                            modifier = Modifier.align(Alignment.End),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Row {
                                // 编辑按钮
                                IconButton(
                                    onClick = {},
                                ) {
                                    Icon(
                                        Icons.TwoTone.Edit,
                                        contentDescription = stringResource(Res.string.edit),
                                    )
                                }

                                // 删除按钮
                                IconButton(
                                    onClick = {},
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Icon(
                                        Icons.TwoTone.Delete,
                                        contentDescription = stringResource(Res.string.delete),
                                    )
                                }
                            }
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
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PomTopAppBar(
        title = {
            Text(
                stringResource(Res.string.otp),
            )
        },
        modifier = modifier,
        navigationIcon = {
            // 返回按钮
            BackIconButton(
                onClick = onBack,
            )
        },
        actions = {
            // 编辑按钮
            IconToggleButton(
                checked = isEditing,
                onCheckedChange = onEditingChange,
            ) {
                Icon(
                    Icons.TwoTone.Edit,
                    contentDescription = stringResource(Res.string.edit),
                )
            }
        },
    )
}

/**
 * OTP 卡片。
 */
@Composable
private fun OtpCard(
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primaryFixed,
        ),
    ) {
        Column {
            Surface(onClick = onClick) {
                ListItem(
                    headlineContent = {
                        headlineContent()
                    },
                    supportingContent = {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.primary,
                            content = supportingContent,
                        )
                    },
                )
            }

            trailingContent?.invoke(this)
        }
    }
}