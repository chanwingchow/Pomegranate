package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import org.koin.compose.viewmodel.koinViewModel

/** 与成绩详情保持一致的弹窗尺寸、字段密度和大屏阅读宽度。 */
@Composable
internal fun AdaptiveStudyDetailScreen(route: AdaptiveStudyDetailRoute, viewModel: StudyDetailViewModel = koinViewModel()) {
    val detail = route.detail
    val backStack = LocalBackStack.current
    val state by viewModel.state.collectAsState()
    val dynamic = detail.teacherId != null || detail.eventId != null
    val sections = if (dynamic) state.takeIf { it.route == detail }?.sections else detail.sections
    val error = state.takeIf { it.route == detail }?.error
    LaunchedEffect(detail) { viewModel.load(detail) }
    val listState = rememberLazyListState()
    val detailLabel = if (detail.id.startsWith("extracurricular:")) when {
        detail.eventId != null -> "活动详情"
        detail.id.startsWith("extracurricular:log:") -> "记录详情"
        else -> "成绩详情"
    } else "课程详情"
    val content: @Composable (Modifier, PaddingValues) -> Unit = { modifier, padding ->
        LazyColumn(modifier, state = listState, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(studyTitleText(detail), style = MaterialTheme.typography.titleLarge)
                    if (detail.summary.isNotBlank()) Text(detail.summary, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                error != null -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ErrorContent(error, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 240.dp))
                        TextButton(onClick = { viewModel.load(detail, force = true) }) { Text("重试") }
                    }
                }
                sections == null -> item { LoadingContent(Modifier.fillMaxWidth().height(160.dp)) }
                sections.isEmpty() -> item { EmptyContent(Modifier.fillMaxWidth().height(160.dp), message = "暂无详细资料") }
                else -> items(sections.filter { it.fields.isNotEmpty() }) { CompactStudySectionContent(it) }
            }
        }
    }
    if (isStudyTwoPane()) Scaffold(topBar = {
        PomTopAppBar(title = { Text(detailLabel) }, actions = {
            IconButton(onClick = { backStack.removeLastOrNull() }) { Icon(Icons.Rounded.Close, "关闭$detailLabel") }
        })
    }, floatingActionButton = { BackToTopButton(listState) }, bottomBar = {
        if (detail.action != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            StudyCourseActionButton(detail, Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            content(Modifier.widthIn(max = 800.dp).fillMaxSize(), padding + PaddingValues(20.dp))
        }
    } else {
        val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .86f }
        Surface(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                content(Modifier.fillMaxWidth().weight(1f, fill = false), PaddingValues(24.dp))
                if (detail.action != null) StudyCourseActionButton(detail, Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp))
            }
        }
    }
}
