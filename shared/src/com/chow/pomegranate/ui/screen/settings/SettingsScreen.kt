package com.chow.pomegranate.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.twotone.AutoMode
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.Res
import com.chow.pomegranate.app_name
import com.chow.pomegranate.auto
import com.chow.pomegranate.dark
import com.chow.pomegranate.demo_mode
import com.chow.pomegranate.dev_by_chow
import com.chow.pomegranate.ic_logo
import com.chow.pomegranate.light
import com.chow.pomegranate.logo
import com.chow.pomegranate.network.WebVpnSession
import com.chow.pomegranate.service.update.PROJECT_URL
import com.chow.pomegranate.service.update.UpdateInstaller
import com.chow.pomegranate.settings
import com.chow.pomegranate.theme_mode
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.PomSettingRow
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.component.rememberCurrentDate
import com.chow.pomegranate.ui.navigation.AlertDialogSceneStrategy
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.ConnectionSettingsRoute
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.ui.theme.ThemeMode
import com.chow.pomegranate.week
import com.chow.pomegranate.week_tooltip
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * 设置页导航入口。
 */
internal fun EntryProviderScope<NavKey>.settingsEntry(updateInstaller: UpdateInstaller?) {
    entry<SettingsRoute> { SettingsScreen() }
    entry<AppUpdateRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { AppUpdateDialog(it, updateInstaller) }
    entry<VersionHistoryRoute> { VersionHistoryScreen() }
    entry<VersionDetailsRoute> { VersionDetailsScreen(it.tag) }
    entry<WeekSettingRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { WeekSettingSheet(it) }
    entry<ClassroomRulesRoute> { ClassroomRulesScreen() }
    entry<ClassroomRuleEditorRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { ClassroomRuleEditorSheet(it) }
    entry<ClassroomBuiltinRulesRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { ClassroomBuiltinRulesSheet(it) }
    entry<ClassroomRuleDeleteRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { ClassroomRuleDeleteDialog(it) }
}

/**
 * 设置页路由。
 */
@Serializable
data object SettingsRoute : NavKey

@Serializable
data class WeekSettingRoute(val week: Int) : NavKey

private data class WeekSelection(val week: Int)

/**
 * 设置页。
 */
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val updates = LocalAppUpdateViewModel.current
    val latest = updates?.latest?.collectAsState()?.value
    val backStack = LocalBackStack.current
    SettingsContent(
        viewModel = viewModel,
        onUpdate = if (latest != null) {
            { if (backStack.lastOrNull() !is AppUpdateRoute) backStack.add(AppUpdateRoute()) }
        } else null,
        startupPromptsEnabled = updates?.startupPromptsEnabled?.collectAsState()?.value ?: true,
        startupPromptsError = updates?.startupPromptsError?.collectAsState()?.value,
        onStartupPromptsChange = updates?.let { it::setStartupPromptsEnabled },
    )
}

@Preview
@Composable
private fun SettingsContentPreview() {
    PomegranateExpressiveTheme {
        SettingsContent()
    }
}


/**
 * 设置页内容。
 */
@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel? = null,
    onUpdate: (() -> Unit)? = null,
    startupPromptsEnabled: Boolean = true,
    startupPromptsError: String? = null,
    onStartupPromptsChange: ((Boolean) -> Unit)? = null,
) {
    val backStack = LocalBackStack.current
    if (viewModel != null) ResultEffect<WeekSelection> { viewModel.setWeek(it.week) }
    val today = rememberCurrentDate()
    LaunchedEffect(today) { viewModel?.updateDate(today) }

    // 打开项目仓库
    val uriHandler = LocalUriHandler.current

    // 高斯模糊
    val sky = rememberSky()

    // 主题模式
    val themeMode = viewModel?.themeMode?.collectAsState()?.value ?: ThemeMode.Auto

    // 周次
    val week = viewModel?.week?.collectAsState()?.value ?: 0
    val demoMode = viewModel?.demoMode?.collectAsState()?.value == true
    val demoModeLabel = stringResource(Res.string.demo_mode)
    val demoModeError = viewModel?.demoModeError?.collectAsState()?.value
    val connection = if (viewModel != null) koinInject<WebVpnSession>() else null
    val connectionState = connection?.state?.collectAsState()?.value
    LaunchedEffect(connection) { connection?.initialize() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLastOrNull()
                },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.sky(sky = sky),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // 应用信息卡片
                AppCard(
                    onOpenProject = { uriHandler.openUri(PROJECT_URL) },
                    onUpdate = onUpdate,
                )
            }

            item {
                // 主题模式
                Card(shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    ThemeModeSetting(themeMode = themeMode, onThemeModeChange = { viewModel?.setThemeMode(it) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }

            item {
                PomSettingRow("版本列表", Icons.Outlined.History, 0, onClick = { backStack.add(VersionHistoryRoute) })
            }

            item {
                // 周次设置
                PomSettingRow(stringResource(Res.string.week), Icons.Rounded.CalendarMonth, 2,
                    trailing = { Text("第 $week 周", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                    onClick = { backStack.add(WeekSettingRoute(week)) })
            }

            item {
                PomSettingRow("连接方式", Icons.Rounded.Language, 3,
                    trailing = { Text(connectionState?.policy?.label ?: "自动", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                    onClick = { backStack.add(ConnectionSettingsRoute) })
            }

            item {
                PomSettingRow("名称替换", Icons.Rounded.Domain, 4,
                    onClick = { backStack.add(ClassroomRulesRoute) })
            }

            if (onStartupPromptsChange != null) item {
                PomSettingRow(
                    "启动时提示更新",
                    Icons.Outlined.Notifications,
                    0,
                    modifier = Modifier.semantics {
                        role = Role.Switch
                        toggleableState = ToggleableState(startupPromptsEnabled)
                    },
                    trailing = { Switch(checked = startupPromptsEnabled, onCheckedChange = null) },
                    onClick = { onStartupPromptsChange(!startupPromptsEnabled) },
                )
                startupPromptsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            item {
                PomSettingRow(demoModeLabel, Icons.Rounded.Science, 1,
                    modifier = Modifier.semantics {
                        role = Role.Switch
                        toggleableState = ToggleableState(demoMode)
                    },
                    trailing = { Switch(checked = demoMode, onCheckedChange = null) },
                    onClick = { viewModel?.setDemoMode(!demoMode) })
                demoModeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

        }
    }
}

/**
 * 顶部导航。
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PomTopAppBar(
        title = {
            Text(
                stringResource(Res.string.settings),
            )
        },
        modifier = modifier,
        navigationIcon = {
            // 返回按钮
            BackIconButton(
                onClick = onBack,
            )
        },
    )
}

/**
 * 应用信息卡片。
 */
@Composable
private fun AppCard(
    onOpenProject: () -> Unit,
    onUpdate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onUpdate ?: onOpenProject,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Logo
                    Surface(
                        shape = MaterialTheme.shapes.small,
                    ) {
                        // Logo（Image 不做主题染色，保留 logo 的渐变）
                        Image(
                            painter = painterResource(Res.drawable.ic_logo),
                            contentDescription = stringResource(Res.string.logo),
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    // 名称
                    Text(
                        stringResource(Res.string.app_name),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (onUpdate != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text("有新版本", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    Text(
                        BuildConfig.versionName,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // GitHub 仓库链接
            Text(
                PROJECT_URL,
                modifier = if (onUpdate != null) Modifier.clickable(onClick = onOpenProject) else Modifier,
                color = MaterialTheme.colorScheme.outlineVariant,
                style = MaterialTheme.typography.labelSmall,
            )

            // 开发者
            Text(
                stringResource(Res.string.dev_by_chow),
                color = MaterialTheme.colorScheme.outlineVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * 主题模式设置。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeSetting(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 标签
    val labels = ThemeMode.entries.associate {
        when (it) {
            ThemeMode.Auto -> it to Res.string.auto
            ThemeMode.Light -> it to Res.string.light
            ThemeMode.Dark -> it to Res.string.dark
        }
    }.mapValues { stringResource(it.value) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 标题
        Text(
            stringResource(Res.string.theme_mode),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )

        // 主题模式选项组
        ButtonGroup(
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
        ) {
            ThemeMode.entries.forEach { mode ->
                val label = labels[mode]!!

                // 主题模式选项
                toggleableItem(
                    checked = mode == themeMode,
                    label = label,
                    onCheckedChange = { onThemeModeChange(mode) },
                    icon = {
                        Icon(
                            when (mode) {
                                ThemeMode.Auto -> Icons.TwoTone.AutoMode
                                ThemeMode.Light -> Icons.TwoTone.LightMode
                                ThemeMode.Dark -> Icons.TwoTone.DarkMode
                            },
                            contentDescription = label,
                        )
                    },
                    weight = 1f,
                )
            }
        }
    }
}

/** 周次独立选择目的地，设置页只展示当前值。 */
@Composable
private fun WeekSettingSheet(route: WeekSettingRoute) {
    var draft by rememberSaveable { mutableIntStateOf(route.week.coerceIn(0, 20)) }
    val backStack = LocalBackStack.current
    val results = LocalResultEventBus.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("设置当前周次", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { draft-- }, enabled = draft > 0) { Icon(Icons.Rounded.Remove, "上一周") }
            Text("第 $draft 周", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            FilledTonalIconButton(onClick = { draft++ }, enabled = draft < 20) { Icon(Icons.Rounded.Add, "下一周") }
        }
        Column {
            Slider(value = draft.toFloat(), onValueChange = { draft = it.roundToInt() }, valueRange = 0f..20f, steps = 19,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "周次" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0 周", style = MaterialTheme.typography.labelMedium)
                Text("20 周", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(stringResource(Res.string.week_tooltip), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = {
            results.sendResult(WeekSelection(draft))
            backStack.removeLastOrNull()
        }, modifier = Modifier.fillMaxWidth()) { Text("应用") }
    }
}
