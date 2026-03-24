package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.app_name
import pomegranate.composeapp.generated.resources.days_of_week

/**
 * 课表页导航入口。
 */
fun EntryProviderScope<NavKey>.timetableEntry(
    liquidState: LiquidState,
    floatingToolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
) {
    entry<TimetableRoute> {
        TimetableScreen(
            liquidState = liquidState,
            toolbarExpanded = floatingToolbarExpanded,
            onToolbarExpandedChange = onToolbarExpandedChange,
        )
    }
}

/**
 * 课表页路由。
 */
@Serializable
data object TimetableRoute : NavKey

/**
 * 课表页。
 */
@Composable
private fun TimetableScreen(
    liquidState: LiquidState,
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
) {
    TimetableContent(
        liquidState = liquidState,
        toolbarExpanded = toolbarExpanded,
        onToolbarExpandedChange = onToolbarExpandedChange,
    )
}

@Preview
@Composable
private fun TimetableContentPreview() {
    PomegranateExpressiveTheme {
        TimetableContent(
            liquidState = rememberLiquidState(),
            toolbarExpanded = true,
            onToolbarExpandedChange = {},
        )
    }
}

/**
 * 课表页内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TimetableContent(
    liquidState: LiquidState,
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sky = rememberSky()

    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.cloudy(sky = sky),
                ) {
                    // 顶部导航栏
                    PomTopAppBar(
                        title = {
                            // 周次
                            TextButton(
                                onClick = {},
                                shape = MaterialShapes.ClamShell.toShape(),
                            ) {
                                Text(
                                    stringResource(Res.string.app_name)
                                )
                            }
                        },
                        shadowElevation = 0.dp,
                    )

                    // 星期几行
                    DaysOfWeekRow(
                        color = MaterialTheme.colorScheme.surface.copy(0.8f),
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(23),
            modifier = Modifier
                .floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { onToolbarExpandedChange(true) },
                    onCollapse = { onToolbarExpandedChange(false) },
                )
                .liquefiable(liquidState)
                .sky(sky = sky),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // 与星期几行的空白间隔
                Spacer(Modifier.height(1.dp))
            }

            for (i in 0..6) {
                item(
                    span = { GridItemSpan(2) },
                ) {
                    // 时间项
                    TimeItem(
                        firstStartTime = { Text("8:00") },
                        firstEndTime = { Text("9:40") },
                        secondStartTime = { Text("10:00") },
                        secondEndTime = { Text("11:40") },
                        modifier = Modifier.height(120.dp),
                    )
                }

                items(
                    7,
                    span = { GridItemSpan(3) },
                ) {
                    // 课程项
                    CourseItem(
                        onClick = {},
                        name = {
                            // 课程名称
                            Text(
                                stringResource(Res.string.app_name),
                            )
                        },
                        classRoom = {
                            // 教室
                            Text(
                                stringResource(Res.string.app_name)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                // 空出悬浮工具栏高度
                Spacer(
                    Modifier.height(FloatingToolbarDefaults.ContainerSize),
                )
            }
        }
    }
}

/**
 * 星期几行。
 */
@Composable
private fun DaysOfWeekRow(
    color: Color = MaterialTheme.colorScheme.surface,
) {
    val dayOfWeeks = stringArrayResource(Res.array.days_of_week)

    Surface(
        color = color,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Spacer(Modifier.weight(2f))

            dayOfWeeks.forEach {
                DayOfWeekItem(
                    dayOfWeek = {
                        Text(it)
                    },
                    datetime = {
                        Text("12/03")
                    },
                    modifier = Modifier.weight(3f),
                )
            }
        }
    }
}

/**
 * 星期几项。
 */
@Composable
private fun DayOfWeekItem(
    dayOfWeek: @Composable () -> Unit,
    datetime: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.tertiary,
            LocalTextStyle provides MaterialTheme.typography.titleSmall,
        ) {
            dayOfWeek()
        }

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.secondaryFixedDim,
            LocalTextStyle provides MaterialTheme.typography.labelSmall,
        ) {
            datetime()
        }
    }
}

/**
 * 时间项。
 */
@Composable
private fun TimeItem(
    firstStartTime: @Composable () -> Unit,
    firstEndTime: @Composable () -> Unit,
    secondStartTime: @Composable () -> Unit,
    secondEndTime: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large.copy(
            topStart = ZeroCornerSize,
            bottomStart = ZeroCornerSize,
        ),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 第一节开始时间
                firstStartTime()
                // 第一节结束时间
                firstEndTime()

                Spacer(Modifier.height(8.dp))

                // 第二节开始时间
                secondStartTime()
                // 第二节结束时间
                secondEndTime()
            }
        }
    }
}

/**
 * 课程项。
 */
@Composable
private fun CourseItem(
    onClick: () -> Unit,
    name: @Composable () -> Unit,
    classRoom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.inversePrimary),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                name()
                classRoom()
            }
        }
    }
}