package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_notice
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 融合搜索栏的顶部导航。
 *
 * 标题栏、搜索栏和 [footer] 共用同一层毛玻璃容器（调用方在 `modifier` 上挂
 * `cloudy`/`liquid` 之类的效果），因此搜索栏看起来是导航的一部分，
 * 而不是飘在内容区的一块独立控件。
 *
 * @param title 标题
 * @param searchBar 搜索栏，`null` 表示不展示
 * @param footer 搜索栏下方的附加内容，例如筛选标签行
 * @param navigationIcon 左侧导航图标
 * @param actions 右侧操作
 * @param shadowElevation 容器阴影
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomSearchTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    searchBar: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    shadowElevation: Dp = 1.dp,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            if (searchBar != null) {
                searchBar()
            }

            if (footer != null) {
                footer()
            }
        }
    }
}

@Preview
@Composable
private fun PomSearchTopBarPreview() {
    PomegranateExpressiveTheme {
        PomSearchTopBar(
            title = { Text(stringResource(Res.string.academic_notice)) },
            searchBar = {
                PomSearchBar(
                    query = "",
                    onQueryChange = {},
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
            footer = {
                Text(
                    "全部 · 标题 · 日期",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        )
    }
}
