package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable

/**
 * 我的页导航入口。
 */
fun EntryProviderScope<NavKey>.meEntry() {
    entry<MeRoute> {
        MeScreen()
    }
}

/**
 * 我的页路由。
 */
@Serializable
data object MeRoute: NavKey

/**
 * 我的页。
 */
@Composable
private fun MeScreen() {
}

@Preview
@Composable
private fun MeContentPreview() {
    PomegranateExpressiveTheme {
        MeContent()
    }
}

/**
 * 我的页内容。
 */
@Composable
private fun MeContent(
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->

    }
}