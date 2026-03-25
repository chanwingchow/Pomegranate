package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable

/**
 * 模块页导航入口。
 */
fun EntryProviderScope<NavKey>.modulesEntry() {
    entry<ModulesRoute> {
        ModulesScreen()
    }
}

/**
 * 模块页路由。
 */
@Serializable
data object ModulesRoute: NavKey

/**
 * 模块页。
 */
@Composable
private fun ModulesScreen() {
    ModulesContent()
}

@Preview
@Composable
private fun ModulesContentPreview() {
    PomegranateExpressiveTheme {
        ModulesContent()
    }
}

/**
 * 模块页内容。
 */
@Composable
private fun ModulesContent(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(120.dp),
            contentPadding = innerPadding,
        ) {

        }
    }
}