package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
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
}