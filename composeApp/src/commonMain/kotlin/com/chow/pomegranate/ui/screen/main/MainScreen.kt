package com.chow.pomegranate.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.ui.component.PomTooltipBox
import com.chow.pomegranate.ui.screen.main.me.MeRoute
import com.chow.pomegranate.ui.screen.main.me.meEntry
import com.chow.pomegranate.ui.screen.main.modules.ModulesRoute
import com.chow.pomegranate.ui.screen.main.modules.modulesEntry
import com.chow.pomegranate.ui.screen.main.timetable.TimetableRoute
import com.chow.pomegranate.ui.screen.main.timetable.timetableEntry
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource

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
@Composable
private fun MainScreen() {
    MainContent()
}

@Preview
@Composable
private fun MainContentPreview() {
    PomegranateExpressiveTheme {
        MainContent()
    }
}

/**
 * 首页内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
) {
    // 悬浮工具栏展开状态
    var floatingToolbarExpanded by remember { mutableStateOf(true) }

    // 返回堆栈
    val backStack = rememberNavBackStack(
        configuration = config,
        TimetableRoute,
    )

    // 液态玻璃效果
    val liquidState = rememberLiquidState()
    val liquidTint = MaterialTheme.colorScheme.surface.copy(0.5f)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.captionBar,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 悬浮工具栏，该组件需要比内容先获取焦点，所以放在此处，
            // 并添加 zIndex 保证在内容上方
            FloatingToolbar(
                expanded = floatingToolbarExpanded,
                onExpandedChange = { floatingToolbarExpanded = it },
                currentRoute = backStack.last(),
                onNavigate = {
                    backStack[0] = it
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                    .zIndex(1f)
                    .clip(CircleShape)
                    .liquid(liquidState) {
                        tint = liquidTint
                    },
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    // 应用液态玻璃，不用默认的容器颜色
                    toolbarContainerColor = Color.Transparent,
                ),
            )

            // 导航
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .padding(innerPadding)
                    .liquefiable(liquidState),
                // 将 ViewModel 范围限定为 NavEntry
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    // 课表页
                    timetableEntry(
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
 *
 * @param expanded 首位内容是否展开
 * @param onExpandedChange 首位内容展开状态变化
 * @param currentRoute 当前路由
 * @param onNavigate 导航事件
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
) {
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = colors,
        leadingContent = {
            // 模块
            val section = MainSection.Modules

            NavToggleButton(
                checked = currentRoute is ModulesRoute,
                onCheckedChange = {
                    onNavigate(ModulesRoute)
                },
                label = stringResource(section.label),
                icon = section.icon,
            )
        },
        trailingContent = {
            // 我
            val section = MainSection.Me

            NavToggleButton(
                checked = currentRoute is MeRoute,
                onCheckedChange = {
                    onNavigate(MeRoute)
                },
                label = stringResource(section.label),
                icon = section.icon,
            )
        },
    ) {
        // 课表
        val section = MainSection.Timetable

        NavToggleButton(
            checked = currentRoute is TimetableRoute,
            onCheckedChange = {
                if (expanded) {
                    onNavigate(TimetableRoute)
                } else {
                    // 点击打开悬浮工具栏
                    onExpandedChange(true)
                }
            },
            label = stringResource(section.label),
            icon = section.icon,
        )
    }
}

/**
 * 导航切换按钮。
 */
@Composable
private fun NavToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    icon: ImageVector,
) {
    PomTooltipBox(
        tooltip = {
            // 工具提示
            PlainTooltip {
                Text(label)
            }
        },
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            // 在上方弹出
            positioning = TooltipAnchorPosition.Above,
        ),
    ) {
        // 填充图标切换按钮
        FilledIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
        ) {
            // 图标
            Icon(
                icon,
                contentDescription = label,
            )
        }
    }
}