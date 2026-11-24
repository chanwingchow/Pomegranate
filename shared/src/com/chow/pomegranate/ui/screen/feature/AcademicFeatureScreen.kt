package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import androidx.compose.ui.text.style.TextOverflow
import com.chow.pomegranate.repository.toStudyCatalog
import com.chow.pomegranate.repository.AcademicFeature
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.AlertDialogSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.DialogListDetailSceneStrategy
import com.chow.pomegranate.ui.navigation.FullWidthOverviewSceneStrategy
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.ui.screen.login.SecondClassLoginRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object CourseSelectionRoute : NavKey
@Serializable data object ExtracurricularRoute : NavKey
@Serializable data object TeachersRoute : NavKey
@Serializable data class TeacherSearchRoute(val query: String) : NavKey
@Serializable data object CourseTimetableRoute : NavKey
@Serializable data object AcademicPlanRoute : NavKey
@Serializable data object AcademicProgressRoute : NavKey
@Serializable data class FeatureDetailRoute(val title: String, val details: Map<String, String>) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.academicFeatureEntries() {
    val listMetadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { StudyDetailPlaceholder() })
    entry<CourseSelectionRoute>(metadata = listMetadata + FullWidthOverviewSceneStrategy.metadata()) { CourseSelectionScreen() }
    entry<CourseOperationRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { CourseOperationDialog(it) }
    entry<SchoolTimetableFiltersRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { SchoolTimetableFiltersSheet(it) }
    entry<SchoolSemesterRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { SchoolSemesterSheet(it) }
    entry<SchoolTimetableDataRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { SchoolTimetableDataSheet() }
    entry<SchoolTimetableDeleteRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { SchoolTimetableDeleteDialog(it) }
    entry<CourseBasketRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { CourseBasketSheet(it) }
    entry<CourseFiltersRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { CourseFiltersSheet(it) }
    entry<CourseInstructionsRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { CourseInstructionsSheet(it) }
    entry<ExtracurricularRoute>(metadata = listMetadata) { StudyCatalogScreen("第二课堂", AcademicFeature.Extracurricular) }
    entry<TeacherSearchRoute>(metadata = listMetadata) { StudyCatalogScreen("教师信息", AcademicFeature.Teachers, initialQuery = it.query) }
    entry<TeachersRoute>(metadata = listMetadata) { StudyCatalogScreen("教师信息", AcademicFeature.Teachers) }
    entry<CourseTimetableRoute>(metadata = listMetadata) { StudyCatalogScreen("全校课表", AcademicFeature.CourseTimetable) }
    entry<AcademicPlanRoute>(metadata = listMetadata) { StudyCatalogScreen("执行计划", AcademicFeature.AcademicPlan) }
    entry<AcademicProgressRoute>(metadata = listMetadata) { StudyCatalogScreen("学业进度", AcademicFeature.AcademicProgress) }
    entry<AdaptiveStudyDetailRoute>(metadata = ListDetailSceneStrategy.detailPane() + DialogListDetailSceneStrategy.metadata()) { AdaptiveStudyDetailScreen(it) }
    entry<StudyDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { StudyDetailScreen(it) }
    entry<FeatureDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
        StudyDetailScreen(StudyDetailRoute("legacy:${it.title}", it.title,
            sections = listOf(StudySection("详情", it.details.map { (label, value) -> StudyField(label, value) }))))
    }
}

@Composable
private fun StudyCatalogScreen(title: String, feature: AcademicFeature, viewModel: StudyCatalogViewModel = koinViewModel(), initialQuery: String = "") {
    val backStack = LocalBackStack.current
    val state by viewModel.state.collectAsState()
    val tabStates by viewModel.tabStates.collectAsState()
    val schoolFilters by viewModel.schoolTimetableFilters.collectAsState()
    var query by rememberSaveable { mutableStateOf(if (feature == AcademicFeature.CourseTimetable) state.query else initialQuery) }
    var initialTermSelected by rememberSaveable { mutableStateOf(false) }
    val remoteSearch = feature == AcademicFeature.Teachers
    LaunchedEffect(feature) { viewModel.ensureLoaded(feature, initialQuery) }
    LaunchedEffect(query) {
        if (remoteSearch) {
            delay(400)
            if (query.trim() != viewModel.state.value.query) viewModel.load(feature, query = query.trim())
        }
    }
    val classroomRules = LocalClassroomReplacements.current
    val courseNames = LocalCourseNameReplacements.current
    val schoolCatalog = remember(state.catalog, query, schoolFilters, classroomRules, courseNames) {
        state.catalog?.schoolTimetable?.let { source -> source.toStudyCatalog(source.courses.filterSchoolTimetable(query, schoolFilters, classroomRules, courseNames)) }
    }
    val groups = (schoolCatalog ?: state.catalog)?.groups.orEmpty()
    val labels = if (feature == AcademicFeature.Extracurricular) listOf("成绩", "活动", "记录") else groups.map { it.title }
    val pager = rememberPagerState { labels.size.coerceAtLeast(1) }
    val scope = rememberCoroutineScope()
    // Map 仅供回顶按钮引用当前页面；实际位置交给各页的 rememberLazyGridState 保存。
    val gridStates = remember { mutableStateMapOf<Int, LazyGridState>() }
    val gridState = gridStates[pager.currentPage]
    if (feature == AcademicFeature.CourseTimetable) ResultEffect<SchoolTimetableFilterResult> { result ->
        if (schoolFilters != result.filters) {
            viewModel.setSchoolTimetableFilters(result.filters)
            gridStates[0]?.requestScrollToItem(0)
        }
    }
    if (feature == AcademicFeature.CourseTimetable) ResultEffect<SchoolSemesterSelection> { result ->
        if (result.semester != state.semester.toString()) {
            if (backStack.lastOrNull().studyDetailRoute()?.id?.startsWith("timetable:") == true) backStack.removeLastOrNull()
            gridStates[0]?.requestScrollToItem(0)
            viewModel.load(feature, semester = com.chow.pomegranate.service.foundation.Semester.parse(result.semester))
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (feature == AcademicFeature.Extracurricular) viewModel.selectTab(pager.settledPage)
    }
    LaunchedEffect(gridState, remoteSearch, state.catalog, state.loading, state.refreshing, state.loadingMore, state.error) {
        if (gridState == null || !remoteSearch || state.loading || state.refreshing || state.loadingMore || state.error != null || state.catalog?.hasMore != true) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: -1) to it.totalItemsCount } }
            .collect { (last, total) -> if (total > 0 && last >= total - 5) viewModel.loadMore() }
    }
    LaunchedEffect(groups.map { it.title }) {
        if (feature == AcademicFeature.AcademicPlan && !initialTermSelected && groups.isNotEmpty()) {
            pager.scrollToPage(groups.indexOfFirst { it.title == state.semester.toString() }.coerceAtLeast(0))
            initialTermSelected = true
        }
    }
    val icon = when (feature) {
        AcademicFeature.Teachers -> Icons.Rounded.Badge
        AcademicFeature.CourseTimetable -> Icons.Rounded.CalendarMonth
        AcademicFeature.AcademicPlan -> Icons.Rounded.RocketLaunch
        AcademicFeature.AcademicProgress -> Icons.AutoMirrored.Rounded.TrendingUp
        else -> Icons.Rounded.Groups
    }
    Scaffold(topBar = {
        StudyTopBar {
            PomTopAppBar(title = { Text(title) }, shadowElevation = 0.dp, navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
                actions = {
                    if (feature == AcademicFeature.CourseTimetable) {
                        IconButton(onClick = { backStack.add(SchoolSemesterRoute(state.semester.toString())) }) { Icon(Icons.Rounded.CalendarMonth, "切换学期") }
                        IconButton(onClick = { backStack.add(SchoolTimetableDataRoute) }) { Icon(Icons.Rounded.Storage, "管理本地课表") }
                    }
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, "刷新") }
                })
            if (labels.size > 1) {
                StudyTabs(labels, pager.currentPage, scrollable = feature == AcademicFeature.AcademicPlan) { i ->
                    scope.launch { pager.animateScrollToPage(i) }
                }
            }
        }
    }, floatingActionButton = { gridState?.let { BackToTopButton(it) } }) { padding ->
        PomPullToRefreshBox(state.refreshing, viewModel::refresh, contentPadding = padding, enabled = state.catalog != null) {
            Column(Modifier.fillMaxSize().padding(padding.withoutBottom())) {
                if (feature != AcademicFeature.AcademicProgress && feature != AcademicFeature.Extracurricular) {
                    StudySearch(query, {
                        query = it
                        if (feature == AcademicFeature.CourseTimetable) {
                            viewModel.setLocalQuery(it)
                            gridStates[0]?.requestScrollToItem(0)
                        }
                    }, when (feature) {
                        AcademicFeature.Teachers -> "搜索教师姓名"
                        AcademicFeature.AcademicPlan -> "搜索课程、编号或开课单位"
                        else -> "搜索课程、教师或教室"
                    },
                        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (feature == AcademicFeature.CourseTimetable) 8.dp else 16.dp))
                }
                if (feature == AcademicFeature.CourseTimetable) state.catalog?.schoolTimetable?.let { source ->
                    Surface(onClick = { backStack.add(SchoolTimetableFiltersRoute(schoolFilters, SchoolTimetableFilterOptions.from(source.courses))) },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.padding(start = 16.dp, end = if (schoolFilters.isActive) 4.dp else 16.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Tune, "筛选全校课表")
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (schoolFilters.isActive) "已筛选课次" else "全部课次", style = MaterialTheme.typography.labelLarge)
                                Text(schoolFilters.copy(classroom = classroomRules.format(schoolFilters.classroom)).summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (schoolFilters.isActive) IconButton(onClick = {
                                viewModel.setSchoolTimetableFilters(SchoolTimetableFilters())
                                gridStates[0]?.requestScrollToItem(0)
                                if ((backStack.lastOrNull() as? StudyDetailRoute)?.id?.startsWith("timetable:") == true) backStack.removeLastOrNull()
                            }) { Icon(Icons.Rounded.Close, "清除课表筛选") }
                            else Icon(Icons.Rounded.ExpandMore, null)
                        }
                    }
                }
                HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
                    val pageState = if (feature == AcademicFeature.Extracurricular) tabStates[page] ?: StudyCatalogState(tab = page) else state
                    val pageGroups = (if (feature == AcademicFeature.CourseTimetable) schoolCatalog else pageState.catalog)?.groups.orEmpty()
                    val pageGridState = rememberLazyGridState()
                    DisposableEffect(page, pageGridState) {
                        gridStates[page] = pageGridState
                        onDispose { if (gridStates[page] === pageGridState) gridStates.remove(page) }
                    }
                    when {
                        pageState.notLoggedIn -> NotLoggedInContent(onLogin = { backStack.add(if (feature == AcademicFeature.Extracurricular) SecondClassLoginRoute else LoginRoute) })
                        pageState.loading -> LoadingContent()
                        pageState.catalog == null && pageState.error != null -> Column(Modifier.fillMaxSize()) {
                            ErrorContent(pageState.error, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.load(feature, tab = page) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("重试") }
                        }
                        else -> {
                            val records = (if (feature == AcademicFeature.Extracurricular) pageGroups.firstOrNull() else pageGroups.getOrNull(page))?.records.orEmpty()
                                .filter { record -> remoteSearch || feature == AcademicFeature.CourseTimetable || query.isBlank() || sequenceOf(record.title, record.subtitle, record.supporting)
                                    .plus(record.detail.sections.flatMap { it.fields }.asSequence().flatMap { field ->
                                        if (field.label in classroomFieldLabels) sequenceOf(field.value, classroomRules.format(field.value)) else sequenceOf(field.value)
                                    }).any { it.contains(query.trim(), true) } ||
                                    feature == AcademicFeature.AcademicPlan && courseNames.matches(record.title, query.trim()) }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                if (records.isEmpty() && pageState.error == null) EmptyContent(message = if (feature == AcademicFeature.CourseTimetable && (schoolFilters.isActive || query.isNotBlank())) "没有符合条件的课次" else if (query.isNotBlank()) "没有匹配的记录" else "暂无记录")
                                else LazyVerticalGrid(columns = GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                                    modifier = Modifier.widthIn(max = 1100.dp).fillMaxSize(), state = pageGridState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (feature in listOf(AcademicFeature.AcademicProgress, AcademicFeature.Extracurricular)) 16.dp else 0.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                                        val recordIcon = if (record.achievement != null) when {
                                            "思想" in record.title || "道德" in record.title -> Icons.Rounded.Verified
                                            "创新" in record.title -> Icons.Rounded.Lightbulb
                                            "志愿" in record.title || "社会" in record.title -> Icons.Rounded.VolunteerActivism
                                            "协作" in record.title || "交流" in record.title -> Icons.Rounded.Forum
                                            "艺术" in record.title || "文化" in record.title -> Icons.Rounded.Palette
                                            "健康" in record.title || "体育" in record.title -> Icons.Rounded.FitnessCenter
                                            "职业" in record.title -> Icons.Rounded.Work
                                            else -> icon
                                        } else icon
                                        StudyRecordCard(record, recordIcon, index,
                                            interactive = feature != AcademicFeature.Extracurricular || page != 0) {
                                            if (feature == AcademicFeature.AcademicPlan || feature == AcademicFeature.Extracurricular) backStack.openAdaptiveStudyDetail(record.detail)
                                            else backStack.openStudyDetail(record.detail)
                                        }
                                    }
                                    if (pageState.error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                                        TextButton(onClick = { if (pageState.catalog?.hasMore == true) viewModel.loadMore() else viewModel.refresh() }, modifier = Modifier.fillMaxWidth()) {
                                            Text("加载失败，点击重试")
                                        }
                                    } else if (remoteSearch) item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            if (pageState.loadingMore) PomLoadingIndicator(size = InlineLoadingIndicatorSize)
                                            else if (pageState.catalog?.hasMore == false && records.isNotEmpty()) Text("已显示全部 ${records.size} 位教师",
                                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
