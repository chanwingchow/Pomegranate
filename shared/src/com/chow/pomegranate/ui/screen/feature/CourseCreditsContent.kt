package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.repository.fields
import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack

/** 学分上限是选课配额，不是需要修满的毕业目标；不合计可能相互包含的分类。 */
@Composable
internal fun CourseCreditsContent(
    value: CourseSelectionUiState.Success, state: LazyGridState, bottomPadding: Dp,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
            state = state, modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (value.overview.credits.progress.isEmpty()) item(key = "no-credit", span = { GridItemSpan(maxLineSpan) }) {
                Text("暂无学分配额信息", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(value.overview.credits.progress, key = { index, _ -> "credit:$index" }) { _, progress -> CreditQuotaCard(progress) }
        }
    }
}

@Composable
internal fun CourseDropLogsContent(
    value: CourseSelectionUiState.Success, state: LazyGridState, bottomPadding: Dp, onRetry: () -> Unit,
) {
    val backStack = LocalBackStack.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1), state = state,
            modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "history-heading", span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.History, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("退课记录", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    value.logs?.let { Text("${it.size} 条", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            when {
                value.loadingLogs -> item(key = "loading-logs", span = { GridItemSpan(maxLineSpan) }) { LoadingContent(Modifier.height(96.dp)) }
                value.logError != null -> item(key = "log-error", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("退课记录加载失败", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRetry) { Text("重试") }
                    }
                }
                value.logs?.isEmpty() == true -> item(key = "empty-logs", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyContent(Modifier.height(160.dp), message = "暂无退课记录")
                }
                else -> value.logs.orEmpty().groupBy { it.dropTime.substringBefore('T').substringBefore(' ') }.forEach { (date, logs) ->
                    item(key = "date:$date", span = { GridItemSpan(maxLineSpan) }) {
                        Text(date.ifBlank { "未提供日期" }, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    itemsIndexed(logs, key = { index, _ -> "log:$date:$index" }, span = { _, _ -> GridItemSpan(maxLineSpan) }) { index, log ->
                        CourseDropRow(log) {
                            backStack.openAdaptiveStudyDetail(log.detailRoute())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditQuotaCard(progress: CreditSummary.CreditProgress) {
    val selected = progress.selected.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val limit = progress.limit.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val exceeded = selected != null && limit != null && selected > limit
    val accent = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(progress.category, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(selected?.let(::credit) ?: progress.selected, style = MaterialTheme.typography.headlineSmall, color = accent)
                Text("已选学分", Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected != null && limit != null) {
                StudyProgress((selected / limit).toFloat())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("上限 ${credit(limit)}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(when { exceeded -> "超出 ${credit(selected - limit)}"; selected == limit -> "已达上限"; else -> "余 ${credit(limit - selected)}" },
                        style = MaterialTheme.typography.labelMedium, color = accent)
                }
            } else Text("上限 ${progress.limit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CourseDropRow(log: CourseDropLog, onClick: () -> Unit) {
    ExpressiveActionCard(onClick = onClick) { _ ->
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(log.courseName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOf(log.teacher, "${credit(log.credits)} 学分").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (log.description.isNotBlank()) Text(log.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                log.dropTime.replace('T', ' ').substringAfter(' ', "").take(5).takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "退课详情", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


internal fun CourseDropLog.detailRoute() = StudyDetailRoute("course-drop:$courseId:$dropTime", courseName,
    summary = "$teacher · ${credit(credits)} 学分", sections = listOf(StudySection("退课详情", fields(
        "退课时间" to dropTime, "退课类型" to dropType, "课程编号" to courseId, "教师" to teacher,
        "课程属性" to courseAttribute, "课程分类" to selectionCategory, "上课安排" to schedule.joinToString("\n"), "操作者" to operator, "说明" to description))))
