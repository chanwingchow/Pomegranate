package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
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