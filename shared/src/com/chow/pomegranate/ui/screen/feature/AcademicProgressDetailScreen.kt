package com.chow.pomegranate.ui.screen.feature

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack

internal enum class ProgressCourseFilter(val label: String) {
    All("全部"), Incomplete("未完成"), Complete("已完成"), Unknown("待确认"),
}

internal data class ProgressCourseRow(val key: String, val course: StudyProgressCourse)

/** 在原始快照上分配稳定键，搜索/筛选不会把某门课的展开状态移给另一门课。 */
internal fun StudyAcademicProgress.filteredCourses(query: String, filter: ProgressCourseFilter): List<ProgressCourseRow> =
    courses.mapIndexed { index, course -> ProgressCourseRow("$index:${course.id}", course) }.filter { (_, course) ->
        (query.isBlank() || listOf(course.name, course.id, course.attribute).any { it.contains(query.trim(), true) }) &&
            when (filter) {
                ProgressCourseFilter.All -> true
                ProgressCourseFilter.Incomplete -> course.completed == false
                ProgressCourseFilter.Complete -> course.completed == true
                ProgressCourseFilter.Unknown -> course.completed == null
            }
    }

@Composable
internal fun AcademicProgressDetailScreen(route: StudyDetailRoute) {
    val progress = route.academicProgress ?: return
    val backStack = LocalBackStack.current
    val pane = isStudyTwoPane()
    var query by rememberSaveable(route.id) { mutableStateOf("") }
    var filter by rememberSaveable(route.id) { mutableIntStateOf(0) }
    var expanded by rememberSaveable(route.id) { mutableStateOf<String?>(null) }
    val rows = remember(progress, query, filter) { progress.filteredCourses(query, ProgressCourseFilter.entries[filter]) }
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val summary = StudyAchievement(progress.earned, progress.required, "学分")
    Scaffold(topBar = {
        PomTopAppBar(title = { Text("学业进度") }, navigationIcon = {
            if (!pane) BackIconButton(onClick = { backStack.removeLastOrNull() })
        }, actions = {
            if (pane) IconButton(onClick = { backStack.removeLastOrNull() }) { Icon(Icons.Rounded.Close, "关闭详情") }
        })
    }, floatingActionButton = { BackToTopButton(listState) }) { padding ->
        // Sticky headers pin to the scrolling viewport, so its top must sit below the app bar.
        Box(Modifier.fillMaxSize().padding(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
        ).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 800.dp).fillMaxSize(), state = listState,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp,
                    bottom = 16.dp + padding.calculateBottomPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "summary") {
                    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(route.title, style = MaterialTheme.typography.titleLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                            itemVerticalAlignment = Alignment.CenterVertically) {
                            Text("${credit(progress.earned)} / ${credit(progress.required)} 学分", style = MaterialTheme.typography.titleMedium)
                            AchievementStatus(summary)
                        }
                        Text(buildString {
                            append("${progress.courses.size} 门课程")
                            if (summary.completed == false) append(" · 还需 ${credit(summary.remaining)} 学分")
                        }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                stickyHeader(key = "filters") {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StudySearch(query, { query = it; listState.requestScrollToItem(1) }, "搜索课程或编号")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProgressCourseFilter.entries.filter { it != ProgressCourseFilter.Unknown || progress.courses.any { c -> c.completed == null } }.forEach { option ->
                                    FilterChip(selected = filter == option.ordinal,
                                        onClick = { filter = option.ordinal; listState.requestScrollToItem(1) }, label = { Text(option.label) })
                                }
                            }
                        }
                    }
                }
                if (rows.isEmpty()) item(key = "empty") { EmptyContent(Modifier.fillMaxWidth().height(240.dp), message = "没有符合条件的课程") }
                items(rows, key = { it.key }) { row ->
                    ProgressCourseItem(row.course, expanded == row.key) { expanded = if (expanded == row.key) null else row.key }
                }
            }
        }
    }
}

@Composable
private fun ProgressCourseItem(course: StudyProgressCourse, expanded: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(courseNameText(course.name), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    if (expanded) "收起课程信息" else "展开课程信息", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(listOf(course.id, course.attribute).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${credit(course.earnedCredits)} / ${credit(course.credits)} 学分", style = MaterialTheme.typography.labelLarge,
                    color = if (course.completed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProgressCourseField("课程编号", course.id.ifBlank { "未提供" })
                            ProgressCourseField("完成情况", when (course.completed) { true -> "已完成"; false -> "未完成"; null -> "学分待确认" })
                            ProgressCourseField("建议修读学期", course.suggestedSemester.ifBlank { "未提供" })
                            ProgressCourseField("免修信息", course.exemption?.takeIf { it.isNotBlank() } ?: "未提供")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCourseField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.8f), style = MaterialTheme.typography.bodyMedium)
    }
}
