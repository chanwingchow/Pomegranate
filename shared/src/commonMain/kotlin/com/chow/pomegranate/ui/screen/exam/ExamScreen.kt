package com.chow.pomegranate.ui.screen.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.campus_canton
import com.chow.pomegranate.campus_foshan
import com.chow.pomegranate.course_id
import com.chow.pomegranate.days_of_week
import com.chow.pomegranate.exam_classroom
import com.chow.pomegranate.exam_schedule
import com.chow.pomegranate.exam_time
import com.chow.pomegranate.no_academic_data
import com.chow.pomegranate.service.academic.affairs.model.CourseExam
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.EmptyContent
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.utils.monthDayFormat
import com.chow.pomegranate.utils.timeFormat
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import com.chow.pomegranate.ui.component.NotLoggedInContent
import com.chow.pomegranate.ui.screen.login.LoginRoute

/**
 * 考试安排页导航入口。
 */
fun EntryProviderScope<NavKey>.examEntry() {
    entry<ExamRoute> {
        ExamScreen()
    }
}

/**
 * 考试安排页路由。
 */
@Serializable
data object ExamRoute : NavKey

/**
 * 考试安排页。
 */
@Composable
private fun ExamScreen() {
    ExamContent()
}

@Preview
@Composable
private fun ExamContentPreview() {
    PomegranateExpressiveTheme {
        ExamContent()
    }
}

/**
 * 考试安排页内容。
 */
@Composable
private fun ExamContent(
    modifier: Modifier = Modifier,
    viewModel: ExamViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current

    // 高斯模糊
    val sky = rememberSky()

    // UI 状态
    val uiState by viewModel.uiState.collectAsState()

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
        val contentPadding = innerPadding + PaddingValues(16.dp)

        when (uiState) {
            is ExamUiState.NotLoggedIn -> {
                // 未登录，不请求受保护接口，直接引导登录
                NotLoggedInContent(
                    onLogin = { backStack.add(LoginRoute) },
                    contentPadding = contentPadding,
                )
            }

            is ExamUiState.Loading -> {
                // 加载中
                LoadingContent(
                    contentPadding = contentPadding,
                )
            }

            is ExamUiState.Error -> {
                // 加载失败
                ErrorContent(
                    throwable = (uiState as ExamUiState.Error).throwable,
                    contentPadding = contentPadding,
                )
            }

            is ExamUiState.Success -> {
                val exams = (uiState as ExamUiState.Success).examSchedule.exams

                if (exams.isEmpty()) {
                    // 暂无考试安排
                    EmptyContent(
                        contentPadding = contentPadding,
                    )
                } else {
                    // 考试列表
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(240.dp),
                        // 顶栏的毛玻璃要采样这里：列表铺满内容区并在顶栏下方延伸
                        modifier = Modifier.fillMaxSize().sky(sky = sky),
                        contentPadding = contentPadding,
                        verticalItemSpacing = 16.dp,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(exams) {
                            CourseExamCard(
                                exam = it,
                            )
                        }
                    }
                }
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
                stringResource(Res.string.exam_schedule),
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
 * 考试卡片。
 *
 * 已结束的考试以卡片自身的内容色变淡表示，不再保留无响应的点击。
 */
@Composable
private fun CourseExamCard(
    exam: CourseExam,
    modifier: Modifier = Modifier,
) {
    // 考试是否已结束
    val isFinished = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()) > exam.endTime

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        CompositionLocalProvider(
            // 已结束的考试整体变淡
            LocalContentColor provides LocalContentColor.current.copy(
                alpha = if (isFinished) 0.38f else LocalContentColor.current.alpha,
            ),
        ) {
            CourseExamBody(exam)
        }
    }
}

/**
 * 考试卡片内容。
 */
@Composable
private fun CourseExamBody(exam: CourseExam) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 考试日期
        ExamDate(
            dateTime = exam.startTime,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 课程名称
            Text(exam.courseName)

            // 考试时间
            ExamTime(
                startTime = exam.startTime,
                endTime = exam.endTime,
            )

            // 考场
            ExamClassroom(
                campus = exam.campus,
                classroom = exam.classroom,
            )

            // 课程编号
            CompositionLocalProvider(
                LocalContentColor provides LocalContentColor.current.copy(
                    alpha = LocalContentColor.current.alpha * 0.4f,
                ),
                LocalTextStyle provides MaterialTheme.typography.labelSmall,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(Res.string.course_id))
                    Text(exam.courseId)
                }
            }
        }
    }
}

/**
 * 考试日期。
 */
@Composable
private fun ExamDate(
    dateTime: LocalDateTime,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 日期
        Text(
            monthDayFormat.format(dateTime.date),
        )

        // 星期
        Text(
            stringArrayResource(Res.array.days_of_week)[dateTime.dayOfWeek.isoDayNumber - 1],
            color = LocalContentColor.current.copy(
                alpha = LocalContentColor.current.alpha * 0.6f,
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * 考试时间。
 */
@Composable
private fun ExamTime(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
) {
    val formatedStartTime = timeFormat.format(startTime.time)
    val formatedEndTime = timeFormat.format(endTime.time)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 时间图标
        Icon(
            Icons.TwoTone.Timer,
            contentDescription = stringResource(Res.string.exam_time),
            modifier = Modifier.size(18.dp),
        )

        // 考试时间
        Text(
            "$formatedStartTime-$formatedEndTime",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * 考场。
 */
@Composable
private fun ExamClassroom(
    campus: Campus,
    classroom: String,
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.TwoTone.LocationOn,
                contentDescription = stringResource(Res.string.exam_classroom),
                modifier = Modifier.size(18.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 考场
                Text(
                    classroom,
                )

                Text("·")

                // 校区
                Text(
                    stringResource(
                        when (campus) {
                            Campus.Canton -> Res.string.campus_canton
                            Campus.Foshan -> Res.string.campus_foshan
                        },
                    ),
                )
            }
        }
    }
}
