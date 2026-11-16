package com.chow.pomegranate.ui.screen.transcript

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.service.academic.affairs.model.LevelGrade
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object TranscriptRoute : NavKey
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.transcriptEntry() {
    entry<TranscriptRoute>(metadata = ListDetailSceneStrategy.listPane(sceneKey = "transcript", detailPlaceholder = { StudyDetailPlaceholder() })) { TranscriptScreen() }
    transcriptDetailEntries()
}

@Composable
private fun TranscriptScreen(viewModel: TranscriptViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val backStack = LocalBackStack.current
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var termMenu by remember { mutableStateOf(false) }
    val courseGrid = rememberLazyGridState()
    val levelGrid = rememberLazyGridState()
    val grid = if (pager.currentPage == 0) courseGrid else levelGrid
    fun dismissDetail() { if (backStack.lastOrNull() is TranscriptDetailRoute) backStack.removeLastOrNull() }
    var settledTab by rememberSaveable { mutableIntStateOf(pager.settledPage) }
    LaunchedEffect(pager.settledPage) {
        if (settledTab != pager.settledPage) dismissDetail()
        settledTab = pager.settledPage
    }
    Scaffold(topBar = {
        StudyTopBar {
            PomTopAppBar(title = { Text("成绩") }, shadowElevation = 0.dp,
                navigationIcon = { BackIconButton(onClick = { dismissDetail(); backStack.removeLastOrNull() }) }, actions = {
                    if (pager.currentPage == 0 && state is TranscriptUiState.Success) IconButton(onClick = {
                        backStack.add(TranscriptOverviewRoute((state as TranscriptUiState.Success).transcripts.course.overview))
                    }) { Icon(Icons.Outlined.Info, "课程概览") }
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, "刷新") }
                })
            StudyTabs(listOf("课程成绩", "等级成绩"), pager.currentPage) { index ->
                dismissDetail()
                scope.launch { pager.animateScrollToPage(index) }
            }
        }
    }, floatingActionButton = { BackToTopButton(grid) }) { padding ->
        PomPullToRefreshBox(refreshing && state is TranscriptUiState.Success, viewModel::refresh, contentPadding = padding,
            enabled = state is TranscriptUiState.Success) {
            when (val current = state) {
                TranscriptUiState.Loading -> LoadingContent(contentPadding = padding)
                TranscriptUiState.NotLoggedIn -> NotLoggedInContent(onLogin = { backStack.add(LoginRoute) }, contentPadding = padding)
                is TranscriptUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorContent(current.throwable, Modifier.weight(1f))
                    TextButton(onClick = viewModel::refresh) { Text("重试") }
                }
                is TranscriptUiState.Success -> {
                    val groups = remember(current.transcripts.course.items, filters.courseKeyword, filters.semester, filters.failedOnly) {
                        current.transcripts.course.items.courseGradeGroups(filters.courseKeyword, filters.semester, filters.failedOnly)
                    }
                    val levelGrades = remember(current.transcripts.level.items, filters.levelKeyword) {
                        current.transcripts.level.items.filterLevelGradesByKeyword(filters.levelKeyword)
                    }
                    HorizontalPager(pager, Modifier.fillMaxSize().padding(padding.withoutBottom()), verticalAlignment = Alignment.Top) { page ->
                        Column(Modifier.fillMaxSize()) {
                            // 留白归固定区域，列表滚动时保持与输入框/筛选的距离。
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StudySearch(if (page == 0) filters.courseKeyword else filters.levelKeyword, {
                                    if (page == 0) { viewModel.submitCourseKeyword(it); courseGrid.requestScrollToItem(0); dismissDetail() }
                                    else { viewModel.submitLevelKeyword(it); levelGrid.requestScrollToItem(0); dismissDetail() }
                                }, if (page == 0) "搜索课程或课程编号" else "搜索考试名称")
                                if (page == 0) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        AssistChip(onClick = { termMenu = true }, label = { Text(filters.semester ?: "全部学期") },
                                            trailingIcon = { Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp)) })
                                        DropdownMenu(termMenu, onDismissRequest = { termMenu = false }) {
                                            val select: (String?) -> Unit = { term ->
                                                viewModel.selectSemester(term); termMenu = false; courseGrid.requestScrollToItem(0); dismissDetail()
                                            }
                                            DropdownMenuItem(text = { Text("全部学期") }, onClick = { select(null) })
                                            current.transcripts.course.items.map { it.semester }.distinct().sortedDescending().forEach { term ->
                                                DropdownMenuItem(text = { Text(term.toString()) }, onClick = { select(term.toString()) })
                                            }
                                        }
                                    }
                                    FilterChip(selected = filters.failedOnly, onClick = {
                                        viewModel.showFailedOnly(!filters.failedOnly); courseGrid.requestScrollToItem(0); dismissDetail()
                                    }, label = { Text("挂科记录") }, leadingIcon = {
                                        Icon(if (filters.failedOnly) Icons.Rounded.Check else Icons.Rounded.FilterAlt, null, Modifier.size(18.dp))
                                    })
                                }
                            }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                if (if (page == 0) groups.isEmpty() else levelGrades.isEmpty()) EmptyContent(
                                    message = if (page == 0) "没有符合条件的课程成绩" else "没有匹配的等级成绩")
                                else LazyVerticalGrid(GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                                    Modifier.widthIn(max = 1100.dp).fillMaxSize(), state = if (page == 0) courseGrid else levelGrid,
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (page == 0) items(groups, key = { it.id }) { group ->
                                        CourseGradesCard(group) { backStack.openTranscriptDetail(it.detailRoute()) }
                                    } else itemsIndexed(levelGrades, key = { index, g -> "${g.name}:${g.date}:$index" }) { index, grade ->
                                        LevelGradeCard(grade, index) { backStack.openTranscriptDetail(grade.detailRoute(index)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CourseGradesCard(group: CourseGradeGroup, onAttempt: (GradeAttempt) -> Unit) {
    val selected = (LocalBackStack.current.lastOrNull() as? TranscriptDetailRoute)?.detail?.id
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.primary)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(courseNameText(group.latest.name), style = MaterialTheme.typography.titleMedium)
                    Text(listOf(group.latest.courseId, "${credit(group.latest.credits)} 学分", group.latest.attribute).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                group.attempts.forEach { attempt ->
                    val grade = attempt.grade
                    val container by animateColorAsState(
                        if (selected == attempt.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.motionScheme.fastEffectsSpec(), label = "Grade selection")
                    Surface(onClick = { onAttempt(attempt) }, shape = MaterialTheme.shapes.medium, color = container) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(grade.examCategory.ifBlank { "课程成绩" }, style = MaterialTheme.typography.labelLarge)
                                Text(grade.semester.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (grade.isFailed) Text("未通过", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            }
                            Text(grade.score, Modifier.widthIn(max = 100.dp), style = MaterialTheme.typography.titleLarge,
                                color = if (grade.isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelGradeCard(grade: LevelGrade, index: Int, onClick: () -> Unit) {
    val selected = (LocalBackStack.current.lastOrNull() as? TranscriptDetailRoute)?.detail?.id == grade.detailRoute(index).detail.id
    ExpressiveActionCard(onClick, selected = selected) { pressed ->
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpressiveIconBadge(Icons.Rounded.Verified, index, pressed)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(grade.name, style = MaterialTheme.typography.titleMedium)
                    Text(grade.date.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(grade.score, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
