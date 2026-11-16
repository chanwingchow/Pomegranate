package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.repository.CourseWorkspaceState
import com.chow.pomegranate.repository.fields
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack

/** 本轮全部已选课程固定展示在一张周表中，不滚动、不按周过滤。 */
@Composable
internal fun CourseSelectionTimetableContent(value: CourseSelectionUiState.Success, work: CourseWorkspaceState,
    bottomPadding: Dp, onRetry: () -> Unit) {
    val timetable = value.timetable
    when {
        value.loadingTimetable || timetable == null && value.timetableError == null -> LoadingContent()
        value.timetableError != null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            ErrorContent(value.timetableError, Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("重试") }
        }
        timetable == null || timetable.rows.all { row -> row.days.all(String::isBlank) } -> EmptyContent(message = "暂无已选课程安排")
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 700.dp
            val cells = remember(timetable, value.overview.selectedCourses) {
                timetable.rows.map { row -> List(7) { day -> selectionTimetableEntries(row.days.getOrElse(day) { "" }, value.overview.selectedCourses) } }
            }
            val stack = LocalBackStack.current
            val selected = stack.lastOrNull().studyDetailRoute()?.id
            Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 16.dp else 8.dp)
                .padding(top = 12.dp, bottom = 12.dp + bottomPadding)) {
                Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("节次", Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    "一二三四五六日".forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(day.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                timetable.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                    val slots = cells[rowIndex]
                    // 每行代表相同的两节课，重叠课程只拆分格子内部，不能改变时间轴比例。
                    Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                            Text(row.sections.replace('-', '–'), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        slots.forEachIndexed { day, entries ->
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                entries.forEachIndexed { entryIndex, entry ->
                                    val detail = entry.course?.record()?.detail?.let { detail ->
                                        detail.copy(action = if (work.expired) null else detail.action?.copy(ownerKey = work.key))
                                    } ?: StudyDetailRoute("selected-table:$rowIndex:$day:$entryIndex", entry.title,
                                        "星期${"一二三四五六日"[day]} · ${row.sections} 节", sections = listOf(StudySection("",
                                            listOf(StudyField("课程安排", entry.text, classroomSchedule = true)))))
                                    TimetableCourseCard(entry.title, entry.supporting, Modifier.fillMaxWidth().weight(1f),
                                        teacher = entry.course?.teacher, wide = wide, selected = selected == detail.id,
                                        compactMetadata = !wide,
                                        onClick = { stack.openAdaptiveStudyDetail(detail) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
