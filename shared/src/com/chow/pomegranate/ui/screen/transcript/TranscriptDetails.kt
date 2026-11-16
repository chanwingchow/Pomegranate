package com.chow.pomegranate.ui.screen.transcript

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.*
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.repository.fields
import com.chow.pomegranate.service.academic.affairs.model.LevelGrade
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.DialogListDetailSceneStrategy
import kotlinx.serialization.Serializable

@Serializable
internal data class TranscriptDetailRoute(val detail: StudyDetailRoute, val score: String, val failed: Boolean = false) : NavKey
@Serializable
internal data class TranscriptOverviewRoute(val description: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.transcriptDetailEntries() {
    entry<TranscriptDetailRoute>(metadata = ListDetailSceneStrategy.detailPane(sceneKey = "transcript") + DialogListDetailSceneStrategy.metadata()) { TranscriptDetail(it) }
    entry<TranscriptOverviewRoute>(metadata = DialogSceneStrategy.dialog()) { route ->
        val backStack = LocalBackStack.current
        // 接口概览为一段原始描述，不拆成指标卡片。
        AlertDialog(onDismissRequest = { backStack.removeLastOrNull() }, title = { Text("课程概览") },
            text = { androidx.compose.foundation.text.selection.SelectionContainer {
                Text(route.description.ifBlank { "暂无课程概览" })
            } }, confirmButton = { TextButton(onClick = { backStack.removeLastOrNull() }) { Text("知道了") } })
    }
}

internal fun MutableList<NavKey>.openTranscriptDetail(route: TranscriptDetailRoute) {
    if (lastOrNull() is TranscriptDetailRoute) removeAt(lastIndex)
    add(route)
}

internal fun GradeAttempt.detailRoute(): TranscriptDetailRoute {
    val g = grade
    return TranscriptDetailRoute(StudyDetailRoute(id, g.name, "${g.semester} · ${g.examCategory}", listOf(
        StudySection("成绩构成", fields("平时成绩" to g.regularScore, "实验成绩" to g.labScore, "期末成绩" to g.finalScore)),
        StudySection("课程信息", fields("课程编号" to g.courseId, "学分" to credit(g.credits), "学时" to credit(g.hours),
            "考核方式" to g.assessment, "课程属性" to g.attribute, "课程性质" to g.category, "通选课分类" to g.electiveCategory,
            "成绩标识" to g.mark, "备注" to g.note)),
    )), g.score, g.isFailed)
}

internal fun LevelGrade.detailRoute(index: Int) = TranscriptDetailRoute(
    StudyDetailRoute("level:$name:$date:$index", name, date.toString()), score,
)

@Composable
private fun TranscriptDetail(route: TranscriptDetailRoute) {
    val pane = isStudyTwoPane()
    val backStack = LocalBackStack.current
    val detail = route.detail
    val content: @Composable (Modifier, PaddingValues) -> Unit = { modifier, padding ->
        LazyColumn(modifier, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (detail.id.startsWith("level:")) detail.title else courseNameText(detail.title), style = MaterialTheme.typography.titleLarge)
                    Text(detail.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.large,
                    color = if (route.failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (detail.id.startsWith("level:")) "考试成绩" else "总评成绩", style = MaterialTheme.typography.labelLarge)
                            if (route.failed) Text("未通过", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(route.score, Modifier.widthIn(max = 160.dp), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            items(detail.sections.filter { it.fields.isNotEmpty() }) { CompactStudySectionContent(it) }
        }
    }
    if (pane) Scaffold(topBar = {
        PomTopAppBar(title = { Text("成绩详情") }, actions = {
            IconButton(onClick = { backStack.removeLastOrNull() }) { Icon(Icons.Rounded.Close, "关闭成绩详情") }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            content(Modifier.widthIn(max = 800.dp).fillMaxSize(), padding + PaddingValues(20.dp))
        }
    }
    else {
        val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .86f }
        Surface(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            content(Modifier.fillMaxWidth().heightIn(max = maxHeight), PaddingValues(24.dp))
        }
    }
}
