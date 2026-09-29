package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.ZeroCornerSize
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.ui.component.EmptyContent
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.datetime.DayOfWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chow.pomegranate.week_of
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.Res
import com.chow.pomegranate.app_name
import com.chow.pomegranate.days_of_week_abbr
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import com.chow.pomegranate.login
import com.chow.pomegranate.login_required
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import androidx.compose.foundation.layout.PaddingValues
import com.chow.pomegranate.ui.component.NotLoggedInContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

/** 每行的节次时间，展示用。 */
private val ROW_TIME_RANGES = listOf(
    "8:00" to "9:40",
    "10:00" to "11:40",
    "14:00" to "15:40",
    "16:00" to "17:40",
    "19:00" to "20:40",
)

/**
 * 课表页路由。
 */
@Serializable
data object TimetableRoute : NavKey

/**
 * 课表页。
 */
@Composable
fun TimetableScreen(
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
) {
    TimetableContent(
        toolbarExpanded = toolbarExpanded,
        onToolbarExpandedChange = onToolbarExpandedChange,
    )
}

@Preview
@Composable
private fun TimetableContentPreview() {
    PomegranateExpressiveTheme {
        TimetableContent(
            toolbarExpanded = true,
            onToolbarExpandedChange = {},
        )
    }
}

/**
 * 课表页内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableContent(
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimetableViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current

    val sky = rememberSky()

    // UI 状态
    val uiState by viewModel.uiState.collectAsState()

    // 周次
    val week by viewModel.week.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                week = week,
                onWeekChange = viewModel::setWeek,
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding

        when (uiState) {
            is TimetableUiState.Error -> {
                // 加载失败
                ErrorContent(
                    throwable = (uiState as TimetableUiState.Error).throwable,
                    contentPadding = contentPadding,
                )
            }

            is TimetableUiState.Loading -> {
                // 加载中
                LoadingContent(
                    contentPadding = contentPadding,
                )
            }

            is TimetableUiState.NotLoggedIn -> {
                // 尚未登录，不请求课表
                NotLoggedInContent(
                    onLogin = { backStack.add(LoginRoute) },
                    contentPadding = contentPadding,
                )
            }

            is TimetableUiState.Success -> {
                val display = (uiState as TimetableUiState.Success).display

                if (display.courses.isEmpty()) {
                    // 暂无课程
                    EmptyContent(
                        contentPadding = contentPadding,
                    )
                } else {
                    // 课表网格
                    TimetableGrid(
                        display = display,
                        toolbarExpanded = toolbarExpanded,
                        onToolbarExpandedChange = onToolbarExpandedChange,
                        sky = sky,
                        contentPadding = contentPadding,
                    )
                }
            }
        }
    }
}

/**
 * 课表网格。
 *
 * @param display 课表展示模型
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableGrid(
    display: TimetableDisplay,
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
    sky: com.skydoves.cloudy.Sky,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(23),
        modifier = Modifier
            .floatingToolbarVerticalNestedScroll(
                expanded = toolbarExpanded,
                onExpand = { onToolbarExpandedChange(true) },
                onCollapse = { onToolbarExpandedChange(false) },
            )
            .sky(sky = sky),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            // 与星期几行的空白间隔
            Spacer(Modifier.height(1.dp))
        }

        for (row in 0..<TIMETABLE_ROW_COUNT) {
            item(
                span = { GridItemSpan(2) },
            ) {
                val (startTime, endTime) = ROW_TIME_RANGES[row]

                // 时间项
                TimeItem(
                    firstStartTime = { Text(startTime) },
                    firstEndTime = { Text(endTime) },
                    modifier = Modifier.height(120.dp),
                )
            }

            items(
                7,
                span = { GridItemSpan(3) },
            ) { column ->
                // 课程项
                CourseItem(
                    courses = display.coursesAt(row, DayOfWeek(column + 1)),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            val height = with(FloatingToolbarDefaults) {
                ContainerSize + ScreenOffset
            }

            // 空出悬浮工具栏高度
            Spacer(
                Modifier.height(height),
            )
        }
    }
}

/**
 * 顶部导航。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBar(
    week: Int,
    onWeekChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 周次菜单是否展开
    var weekMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(0.8f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier,
        ) {
            // 顶部导航
            PomTopAppBar(
                title = {
                    Box {
                        // 周次
                        TextButton(
                            onClick = { weekMenuExpanded = true },
                            shape = MaterialShapes.ClamShell.toShape(),
                        ) {
                            Text(
                                stringResource(Res.string.week_of, week),
                            )
                        }

                        DropdownMenu(
                            expanded = weekMenuExpanded,
                            onDismissRequest = { weekMenuExpanded = false },
                        ) {
                            // 第 0 周表示开学前一周
                            for (index in 0 until TIMETABLE_MAX_WEEK) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(Res.string.week_of, index))
                                    },
                                    onClick = {
                                        onWeekChange(index)
                                        weekMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                shadowElevation = 0.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            // 星期几行
            DaysOfWeekRow(week = week)
        }
    }
}

/**
 * 星期几行。
 */
@Composable
private fun DaysOfWeekRow(
    week: Int,
    modifier: Modifier = Modifier,
) {
    val dayOfWeeks = stringArrayResource(Res.array.days_of_week_abbr)

    // 该周每天的日期，第 0 周表示本周
    val dates = remember(week) { weekDates(week) }

    // 今天，用于标记所在列
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Spacer(Modifier.weight(2f))

        dayOfWeeks.forEachIndexed { index, dayOfWeek ->
            val date = dates.getOrNull(index)

            DayOfWeekItem(
                dayOfWeek = {
                    Text(dayOfWeek)
                },
                datetime = {
                    Text(date?.let(dateFormat::format).orEmpty())
                },
                // 只在本周标记今天，其他周不高亮
                isToday = week == 0 && date == today,
                modifier = Modifier.weight(3f),
            )
        }
    }
}

/**
 * 返回第 [week] 周 7 天的日期。
 *
 * 第 0 周为本周，每加一周向前推 7 天，与周次偏好的含义保持一致。
 */
private fun weekDates(week: Int): List<LocalDate> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // 本周周一
    val daysFromMonday = today.dayOfWeek.isoDayNumber - 1
    val monday = today.minus(DatePeriod(days = daysFromMonday + week * 7))

    return (0..6).map { monday.plus(DatePeriod(days = it)) }
}

/** 日期格式，如 09/02。 */
private val dateFormat by lazy {
    LocalDate.Format {
        monthNumber()
        char('/')
        day()
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
    isToday: Boolean = false,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 今天用实心圆突出显示
        Surface(
            shape = CircleShape,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
            contentColor = if (isToday) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleSmall,
                ) {
                    dayOfWeek()
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        CompositionLocalProvider(
            LocalContentColor provides if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryFixedDim
            },
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
                // 开始时间
                firstStartTime()
                // 结束时间
                firstEndTime()
            }
        }
    }
}

/**
 * 课程项。
 *
 * 同一个时间格内可能有多门课程。
 */
@Composable
private fun CourseItem(
    courses: List<TimetableCourse>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (courses.isEmpty()) {
            // 无课的时间格留白
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        border = if (courses.isEmpty()) {
            null
        } else {
            BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.inversePrimary,
            )
        },
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                courses.forEach { course ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 课程名称
                        Text(
                            course.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // 教室
                        course.classroom?.let {
                            Text(
                                it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
