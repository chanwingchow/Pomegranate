package com.chow.pomegranate.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Face4
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.ui.screen.main.me.MeRoute
import com.chow.pomegranate.ui.screen.main.me.meEntry
import com.chow.pomegranate.ui.screen.main.modules.ModulesRoute
import com.chow.pomegranate.ui.screen.main.modules.modulesEntry
import com.chow.pomegranate.ui.screen.main.timetable.TimetableRoute
import com.chow.pomegranate.ui.screen.main.timetable.timetableEntry
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * 主页导航入口。
 */
fun EntryProviderScope<NavKey>.mainEntry() {
    entry<MainRoute> {
        MainScreen()
    }
}

/**
 * 主页路由。
 */
@Serializable
data object MainRoute : NavKey

/** 序列化配置 */
private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TimetableRoute::class)
            subclass(ModulesRoute::class)
            subclass(MeRoute::class)
        }
    }
}

/**
 * 主页。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
private fun MainScreen() {
    // 悬浮工具栏展开状态
    var floatingToolbarExpanded by remember { mutableStateOf(true) }

    // 返回堆栈
    val backStack = rememberNavBackStack(
        configuration = config,
        TimetableRoute,
    )

    // 高斯模糊效果
    val hazeState = rememberHazeState()

    Scaffold(
        contentWindowInsets = WindowInsets.captionBar,
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding),
        ) {
            // 悬浮工具栏，该组件需要比内容先获取焦点，所以放在此处，
            // 并添加 zIndex 保证在内容上方
            FloatingToolbar(
                expanded = floatingToolbarExpanded,
                onExpandedChange = { floatingToolbarExpanded = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                    .zIndex(1f)
                    .clip(CircleShape)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.ultraThin(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.9f),
                        ),
                    ),
            )

            // 导航
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .padding(innerPadding),
                // 将 ViewModel 范围限定为 NavEntry
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    // 课表页
                    timetableEntry(
                        hazeState = hazeState,
                        floatingToolbarExpanded = floatingToolbarExpanded,
                        onToolbarExpandedChange = { floatingToolbarExpanded = it },
                    )
                    // 模块页
                    modulesEntry()
                    // 我的页
                    meEntry()
                },
            )
        }
    }
}

/**
 * 悬浮工具栏。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = Color.Transparent,
        ),
        leadingContent = {
            FilledTonalButton(
                onClick = {},
            ) {
                Icon(
                    Icons.Rounded.ViewModule,
                    contentDescription = null,
                )
            }
        },
        trailingContent = {
            FilledTonalButton(
                onClick = {},
            ) {
                Icon(
                    Icons.Rounded.Face4,
                    contentDescription = null,
                )
            }
        },
    ) {
        FilledIconToggleButton(
            checked = true,
            onCheckedChange = {
                // 切换选中状态

                // 点击打开悬浮工具栏
                onExpandedChange(true)
            },
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
            )
        }
    }
}