package com.chow.pomegranate.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.twotone.AutoMode
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.ui.component.PomTooltipBox
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.ui.theme.ThemeMode
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.app_name
import pomegranate.composeapp.generated.resources.auto
import pomegranate.composeapp.generated.resources.dark
import pomegranate.composeapp.generated.resources.dev_by_chow
import pomegranate.composeapp.generated.resources.ic_logo
import pomegranate.composeapp.generated.resources.light
import pomegranate.composeapp.generated.resources.logo
import pomegranate.composeapp.generated.resources.nav_before
import pomegranate.composeapp.generated.resources.settings
import pomegranate.composeapp.generated.resources.theme_mode
import pomegranate.composeapp.generated.resources.tips
import pomegranate.composeapp.generated.resources.week
import pomegranate.composeapp.generated.resources.week_tooltip

/**
 * 设置页导航入口。
 */
fun EntryProviderScope<NavKey>.settingsEntry() {
    entry<SettingsRoute> {
        SettingsScreen()
    }
}

/**
 * 设置页路由。
 */
@Serializable
data object SettingsRoute : NavKey

/**
 * 设置页。
 */
@Composable
fun SettingsScreen() {
    SettingsContent()
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
) {
    val backStack = LocalBackStack.current

    val sky = rememberSky()

    // 主题模式
    var themeMode by remember { mutableStateOf(ThemeMode.Auto) }

    // 周次
    var week by remember { mutableIntStateOf(1) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLast()
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
                    onClick = {},
                )
            }

            item {
                // 主题模式
                ThemeModeSetting(
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                )
            }

            item {
                // 周次设置
                WeekSetting(
                    week = week,
                    onWeekChange = { week = it },
                )
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
            IconButton(
                onClick = onBack,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.NavigateBefore,
                    contentDescription = stringResource(Res.string.nav_before),
                )
            }
        },
    )
}

/**
 * 应用信息卡片。
 */
@Composable
private fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
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
                    Image(
                        painterResource(Res.drawable.ic_logo),
                        contentDescription = stringResource(Res.string.logo),
                        modifier = Modifier.size(28.dp).clip(MaterialTheme.shapes.small),
                    )

                    // 名称
                    Text(
                        stringResource(Res.string.app_name),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // 版本号
                Text(
                    BuildConfig.versionName,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // GitHub 仓库链接
            Text(
                "https://github.com/chanwingchow/Pomegranate",
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

/**
 * 周次设置。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeekSetting(
    week: Int,
    onWeekChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val tooltipState = rememberTooltipState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 标题
            Text(
                stringResource(Res.string.week),
                modifier = Modifier.padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )

            PomTooltipBox(
                tooltip = {
                    // 工具提示
                    PlainTooltip {
                        Text(stringResource(Res.string.week_tooltip))
                    }
                },
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    // 在上方弹出
                    positioning = TooltipAnchorPosition.Above,
                ),
                state = tooltipState,
                enableUserInput = false,
            ) {
                // 提示
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(Res.string.tips),
                    modifier = Modifier.size(16.dp).clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            scope.launch {
                                tooltipState.show()
                            }
                        },
                    ),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }

        // 周次列表
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(21) { index ->
                // 周次选项
                ToggleButton(
                    checked = index == week,
                    onCheckedChange = { onWeekChange(index) },
                ) {
                    Text("$index")
                }
            }
        }
    }
}