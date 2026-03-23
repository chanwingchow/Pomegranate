package com.chow.pomegranate

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.ui.screen.main.MainRoute
import com.chow.pomegranate.ui.screen.main.mainEntry
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 序列化配置 */
private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute::class)
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
@Preview
fun App() {
    PomegranateExpressiveTheme {
        val backStack = rememberNavBackStack(
            configuration = savedStateConfiguration,
            MainRoute,
        )

        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                // 主页
                mainEntry()
            },
        )
    }
}