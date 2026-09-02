package com.chow.pomegranate.ui.screen.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.chow.pomegranate.service.academic.affairs.model.CourseExam
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.utils.monthDayFormat
import com.chow.pomegranate.utils.timeFormat
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

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
) {
    val backStack = LocalBackStack.current

    // 高斯模糊
    val sky = rememberSky()

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    // TODO: 模拟数据
    val exams = ExamSchedule(
        "",
        Semester.now(),
        List(40) {
            CourseExam(
                "2024",
                "大学语文",
                now,
                now,
                Campus.Canton,
                "北5 123",
            )
        },
    )

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
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(240.dp),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(exams.exams) {
                CourseExamCard(
                    exam = it,
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
 */
@Composable
private fun CourseExamCard(
    exam: CourseExam,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = {},
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.38f),
        ),
        enabled = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()) <= exam.endTime,
    ) {
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
            "${formatedStartTime}-${formatedEndTime}",
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