package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 保留页面内容和滚动位置，使用 Material3 Expressive 原生下拉刷新指示器。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(isRefreshing, onRefresh, modifier.fillMaxSize(), state = state, enabled = enabled,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(state, isRefreshing,
                Modifier.align(Alignment.TopCenter).padding(top = contentPadding.calculateTopPadding()))
        }, content = content)
}
