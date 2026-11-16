package com.chow.pomegranate.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.chow.pomegranate.ui.component.PomTooltipBox
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.popDestination
import com.chow.pomegranate.ui.navigation.selectTopLevel
import com.chow.pomegranate.ui.screen.main.me.MeRoute
import com.chow.pomegranate.ui.screen.main.me.MeScreen
import com.chow.pomegranate.ui.screen.main.modules.ModulesRoute
import com.chow.pomegranate.ui.screen.main.modules.ModulesScreen
import com.chow.pomegranate.ui.screen.main.timetable.TimetableNotesRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableCourseRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableCourseSheet
import com.chow.pomegranate.ui.screen.main.timetable.TimetableWeekRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableWeekSheet
import com.chow.pomegranate.ui.screen.main.timetable.TimetableDateRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableDateDialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.scene.DialogSceneStrategy
import com.chow.pomegranate.ui.screen.main.timetable.TimetableEditRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableEditSheet
import com.chow.pomegranate.ui.screen.main.timetable.TIMETABLE_DETAIL_METADATA
import com.chow.pomegranate.ui.screen.main.timetable.TIMETABLE_MAIN_METADATA
import com.chow.pomegranate.ui.screen.main.timetable.TimetableNotesSheet
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.screen.main.timetable.TimetableRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableScreen
import com.chow.pomegranate.ui.screen.main.timetable.TimetableViewModel
import com.chow.pomegranate.session.SessionManager
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.collectAsState
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.ui.screen.main.me.Gender
import org.koin.compose.koinInject

/**
 * 主页导航入口。
 */
fun EntryProviderScope<NavKey>.mainEntry() {
    entry<TimetableNotesRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { TimetableNotesSheet(it) }
    entry<TimetableWeekRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { TimetableWeekSheet(it) }
    entry<TimetableDateRoute>(metadata = DialogSceneStrategy.dialog(DialogProperties(usePlatformDefaultWidth = false))) { TimetableDateDialog(it) }
    entry<TimetableEditRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { TimetableEditSheet(it) }
    entry<TimetableCourseRoute>(metadata = BottomSheetSceneStrategy.bottomSheet() + mapOf(TIMETABLE_DETAIL_METADATA to true)) { TimetableCourseSheet(it) }
    entry<MainRoute>(metadata = mapOf(TIMETABLE_MAIN_METADATA to true)) {
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
 * 主页内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
) {
    // 悬浮工具栏展开状态
    var floatingToolbarExpanded by rememberSaveable { mutableStateOf(true) }

    // 性别："我"的图标需要跟随它变化
    val preferenceRepository = koinInject<AppPreferenceRepository>()
    val gender = preferenceRepository.observe(PreferenceKeys.GENDER)
        .collectAsState(initial = null)
        .value
        ?.toIntOrNull()
        ?.let { ordinal -> Gender.entries.getOrNull(ordinal) }
        ?: Gender.Default

    // 返回堆栈
    val backStack = rememberNavBackStack(
        configuration = config,
        TimetableRoute,
    )
    val appBackStack = LocalBackStack.current
    // 随主页面保留课表状态，切换栏目不会销毁；账号切换时使用独立的状态。
    val session = koinInject<SessionManager>()
    val userId by session.userId.collectAsState()
    val accounts by session.accounts.collectAsState()
    val demoAccount = accounts.any { it.isActive && it.isDemo }
    val timetableViewModel = koinViewModel<TimetableViewModel>(key = "main-timetable:$userId:$demoAccount")
    val onNavigate: (NavKey) -> Unit = { destination ->
        if (destination != TimetableRoute && appBackStack.lastOrNull() is TimetableCourseRoute) appBackStack.removeLastOrNull()
        backStack.selectTopLevel(destination)
    }
    val isWideNavigation = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    // 液态玻璃效果
    val liquidState = rememberLiquidState()
    val liquidTint = MaterialTheme.colorScheme.surface.copy(0.5f)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.captionBar,
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isWideNavigation) {
                MainNavigationRail(
                    currentRoute = backStack.last(),
                    onNavigate = onNavigate,
                    gender = gender,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                if (!isWideNavigation) {
                    FloatingToolbar(
                        expanded = floatingToolbarExpanded,
                        onExpandedChange = { floatingToolbarExpanded = it },
                        currentRoute = backStack.last(),
                        onNavigate = onNavigate,
                        gender = gender,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                            .zIndex(1f)
                            .clip(CircleShape)
                            .liquid(liquidState) { tint = liquidTint }
                            .border(
                                width = 0.2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = Color.Transparent,
                        ),
                    )
                }

                NavDisplay(
                    // 与根导航保持相同的场景交接，避免顶层页面切换留下旧内容。
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    popTransitionSpec = { fadeIn() togetherWith fadeOut() },
                    backStack = backStack,
                    onBack = { backStack.popDestination() },
                    modifier = Modifier
                        .fillMaxSize()
                        // 宽屏使用导航轨，无玻璃工具栏，无需把整页录制到离屏图层。
                        .then(if (isWideNavigation) Modifier else Modifier.liquefiable(liquidState)),
                    // 将 ViewModel 范围限定为 NavEntry
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        // 课表页
                        entry<TimetableRoute> {
                            TimetableScreen(
                                viewModel = timetableViewModel,
                                toolbarExpanded = floatingToolbarExpanded,
                                onToolbarExpandedChange = { floatingToolbarExpanded = it },
                            )
                        }
                        // 模块页
                        entry<ModulesRoute> {
                            ModulesScreen()
                        }
                        // 我的页
                        entry<MeRoute> {
                            MeScreen(
                                toolbarExpanded = floatingToolbarExpanded,
                                onToolbarExpandedChange = { floatingToolbarExpanded = it },
                            )
                        }
                    },
                )
            }
        }
    }
}

/** 宽屏设备使用靠近屏幕边缘的导航栏。 */
@Composable
private fun MainNavigationRail(
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    gender: Gender,
) {
    NavigationRail {
        MainSection.entries.forEach { section ->
            val route = when (section) {
                MainSection.Modules -> ModulesRoute
                MainSection.Timetable -> TimetableRoute
                MainSection.Me -> MeRoute
            }
            NavigationRailItem(
                selected = currentRoute::class == route::class,
                onClick = { onNavigate(route) },
                icon = { Icon(section.icon(gender), contentDescription = null) },
                label = { Text(stringResource(section.label)) },
                alwaysShowLabel = true,
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
    gender: Gender,
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
                icon = section.icon(gender),
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
                icon = section.icon(gender),
            )
        },
    ) {
        MainSection.entries.forEach { section ->
            val checked = when (section) {
                MainSection.Modules -> currentRoute is ModulesRoute
                MainSection.Timetable -> currentRoute is TimetableRoute
                MainSection.Me -> currentRoute is MeRoute
            }

            if (expanded && section == MainSection.Timetable || !expanded && checked) {
                AnimatedVisibility(true) {
                    NavToggleButton(
                        checked = checked,
                        onCheckedChange = {
                            if (expanded) {
                                onNavigate(TimetableRoute)
                            } else {
                                // 点击打开悬浮工具栏
                                onExpandedChange(true)
                            }
                        },
                        label = stringResource(section.label),
                        icon = section.icon(gender),
                    )
                }
            }
        }
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
