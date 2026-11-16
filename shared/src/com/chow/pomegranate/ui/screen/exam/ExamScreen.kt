package com.chow.pomegranate.ui.screen.exam

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.service.academic.affairs.model.CourseExam
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.utils.timeFormat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Serializable data object ExamRoute : NavKey
fun EntryProviderScope<NavKey>.examEntry() { entry<ExamRoute> { ExamScreen() } }

@Composable
private fun ExamScreen(viewModel: ExamViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val backStack = LocalBackStack.current
    var filter by rememberSaveable { mutableIntStateOf(1) }
    val gridState = rememberLazyGridState()
    val layoutDirection = LocalLayoutDirection.current
    Scaffold(topBar = { StudyTopBar {
        PomTopAppBar(title = { Text("考试安排") }, shadowElevation = 0.dp, navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
            actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, "刷新") } })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(3) { index ->
                FilterChip(filter == index, onClick = { filter = index }, label = { Text(listOf("全部", "待考", "已结束")[index]) },
                    leadingIcon = { if (filter == index) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) })
            }
        }
    } },
        floatingActionButton = { BackToTopButton(gridState) }) { padding ->
        PomPullToRefreshBox(refreshing && state is ExamUiState.Success, viewModel::refresh, contentPadding = padding,
            enabled = state is ExamUiState.Success) {
            when (val current = state) {
                ExamUiState.Loading -> LoadingContent(contentPadding = padding)
                ExamUiState.NotLoggedIn -> NotLoggedInContent(onLogin = { backStack.add(LoginRoute) }, contentPadding = padding)
                is ExamUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorContent(current.throwable, Modifier.weight(1f)); TextButton(onClick = viewModel::refresh) { Text("重试") }
                }
                is ExamUiState.Success -> {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val exams = current.examSchedule.exams.filter {
                        when (filter) { 1 -> it.endTime > now; 2 -> it.endTime <= now; else -> true }
                    }.sortedBy { it.startTime }
                    Box(Modifier.fillMaxSize().padding(
                        start = padding.calculateStartPadding(layoutDirection), top = padding.calculateTopPadding(),
                        end = padding.calculateEndPadding(layoutDirection),
                    ).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
                        if (exams.isEmpty()) EmptyContent(contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                            message = when (filter) { 1 -> "暂无待考考试"; 2 -> "暂无已结束的考试"; else -> "暂无考试安排" })
                        else LazyVerticalGrid(GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                            Modifier.widthIn(max = 1100.dp).fillMaxSize(), state = gridState,
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(current.examSchedule.semester.toString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${exams.size} 场考试", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else Text("${current.examSchedule.semester} · ${exams.size} 场考试", style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            itemsIndexed(exams) { index, exam -> ExamCard(exam, index) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: CourseExam, index: Int) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    val time = "${timeFormat.format(exam.startTime.time)}–${timeFormat.format(exam.endTime.time)}"
    val room = classroomText(exam.classroom)
    val location = "${if (exam.campus == Campus.Canton) "广州校区" else "佛山校区"} · $room"
    val information: @Composable () -> Unit = {
        if (largeText) {
            Text(exam.startTime.date.toString(), style = MaterialTheme.typography.bodyLarge)
            Text(time, style = MaterialTheme.typography.bodyLarge)
        } else Text("${exam.startTime.date} · $time", style = MaterialTheme.typography.bodyLarge)
        Text(location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (room != exam.classroom) Text("原始教室：${exam.classroom}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("课程编号 ${exam.courseId}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = if (largeText) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpressiveIconBadge(Icons.Rounded.Event, index, false, Modifier.size(36.dp), iconSize = 20.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(courseNameText(exam.courseName), style = MaterialTheme.typography.titleMedium)
                    if (!largeText) information()
                }
            }
            if (largeText) information()
        }
    }
}
