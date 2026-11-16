package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StudyDetailScreen(route: StudyDetailRoute, viewModel: StudyDetailViewModel = koinViewModel()) {
    if (route.academicProgress != null) {
        AcademicProgressDetailScreen(route)
        return
    }
    val backStack = LocalBackStack.current
    val state by viewModel.state.collectAsState()
    val sections = if (route.teacherId == null && route.eventId == null) route.sections else state.takeIf { it.route == route }?.sections
    val error = state.takeIf { it.route == route }?.error
    LaunchedEffect(route) { viewModel.load(route) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val twoPane = isStudyTwoPane()
    Scaffold(topBar = {
        PomTopAppBar(title = { Text("详情") }, navigationIcon = {
            if (!twoPane) BackIconButton(onClick = { backStack.removeLastOrNull() })
        })
    }, floatingActionButton = { BackToTopButton(listState) }, bottomBar = {
        if (route.action != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            StudyCourseActionButton(route, Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                error != null -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorContent(error, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.load(route, force = true) }) { Text("重试") }
                }
                sections == null -> LoadingContent(contentPadding = padding)
                sections.isEmpty() -> EmptyContent(contentPadding = padding, message = "暂无详细资料")
                else -> LazyColumn(Modifier.widthIn(max = 800.dp).fillMaxSize(),
                    state = listState, contentPadding = padding + PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(studyTitleText(route), style = MaterialTheme.typography.headlineSmall)
                            if (route.summary.isNotBlank()) Text(route.summary, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(sections.filter { it.fields.isNotEmpty() }) { StudySectionContent(it) }
                }
            }
        }
    }
}
