package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.chow.pomegranate.repository.*
import androidx.compose.material.icons.outlined.StarBorder
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.repository.fields
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable enum class CourseOperation { Select, Drop }
@Serializable data class CourseOperationRoute(val operation: CourseOperation, val courseId: String, val courseName: String,
    val category: String = CourseSelectCategory.General.name, val summary: String = "",
    val items: List<CourseCartItem> = emptyList(), val ownerKey: String = "") : NavKey
@Serializable data class CourseInstructionsRoute(val section: StudySection) : NavKey

private enum class CourseSelectionTab(val label: String) {
    Favorites("收藏"), Select("选课"), Selected("已选课程"), Timetable("课表"), Credits("学分"), DropLogs("退课记录")
}

internal val CourseSelectCategory.label: String get() = when (this) {
    CourseSelectCategory.Basic -> "基础与必修课"
    CourseSelectCategory.Optional -> "选修课"
    CourseSelectCategory.General -> "通识课"
    CourseSelectCategory.Professional -> "专业计划课"
    CourseSelectCategory.CrossGrade -> "跨年级"
    CourseSelectCategory.Interprofessional -> "跨专业"
}

@Composable
internal fun CourseSelectionScreen(viewModel: CourseSelectionViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val state by viewModel.uiState.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val operationNotice by viewModel.operationNotice.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    val fallbackScope = rememberCoroutineScope()
    val feedbackScope = LocalAppCoroutineScope.current ?: fallbackScope
    val workspace = viewModel.workspace
    val work by workspace.state.collectAsState()
    val pager = rememberPagerState(initialPage = CourseSelectionTab.Select.ordinal) { CourseSelectionTab.entries.size }
    var favoriteQuery by rememberSaveable { mutableStateOf("") }
    var dropQuery by rememberSaveable { mutableStateOf("") }
    var isSelecting by rememberSaveable { mutableStateOf(false) }
    val favoriteGridState = rememberLazyGridState()
    val tab = CourseSelectionTab.entries[pager.currentPage]
    var query by rememberSaveable { mutableStateOf("") }
    val searchGridState = rememberLazyGridState()
    val selectedGridState = rememberLazyGridState()
    val infoState = rememberLazyGridState()
    val logState = rememberLazyGridState()
    val gridState = when (tab) {
        CourseSelectionTab.Favorites -> favoriteGridState
        CourseSelectionTab.Select -> searchGridState
        CourseSelectionTab.Selected -> selectedGridState
        CourseSelectionTab.Credits -> infoState
        CourseSelectionTab.DropLogs -> logState
        CourseSelectionTab.Timetable -> selectedGridState
    }
    val current = state as? CourseSelectionUiState.Success
    val drop = tab == CourseSelectionTab.Selected
    val batchEnabled = tab == CourseSelectionTab.Favorites || drop
    val pending = if (drop) work.pendingDrop else work.pendingSelect
    val batchItems = if (work.expired) emptyList() else when (tab) {
        CourseSelectionTab.Favorites -> work.favorites.filter { it.matches(favoriteQuery) }.map { (course, category) ->
            course.copy(isSelected = work.selectionOverrides[course.id] ?: (course.isSelected || course.id in work.selectedIds || work.submittedPreferences.containsKey("${course.courseId}:${course.id}")))
                .record(CourseSelectCategory.valueOf(category), work.preferenceMode).detail.let { detail -> detail.copy(action = detail.action?.copy(ownerKey = work.key)) }
        }.filter { it.action?.drop == false }.mapNotNull { it.cartItem() }
        CourseSelectionTab.Selected -> current?.overview?.selectedCourses.orEmpty().filter { it.matches(dropQuery) }
            .mapNotNull { it.record().detail.let { detail -> detail.copy(action = detail.action?.copy(ownerKey = work.key)).cartItem() } }
        else -> emptyList()
    }
    val allSelected = batchItems.isNotEmpty() && batchItems.all { c -> pending.any { it.id == c.id } }
    fun finishSelection() {
        if (work.running) return
        isSelecting = false
        workspace.clearPending(drop)
    }
    fun beginSelection(item: CourseCartItem? = null) {
        if (work.running || work.expired) return
        if (backStack.lastOrNull().studyDetailRoute() != null) backStack.removeLastOrNull()
        if (!isSelecting) workspace.clearPending(drop)
        isSelecting = true
        item?.let { workspace.togglePending(it, drop) }
    }
    NavigationBackHandler(state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = isSelecting && backStack.lastOrNull() == CourseSelectionRoute,
        onBackCompleted = { finishSelection() })
    var selectionPage by rememberSaveable { mutableIntStateOf(pager.settledPage) }
    var selectionOwner by rememberSaveable { mutableStateOf(work.key) }
    LaunchedEffect(pager.settledPage, work.key) {
        if (selectionPage != pager.settledPage || selectionOwner != work.key) {
            isSelecting = false
            workspace.clearPending(false)
            workspace.clearPending(true)
        }
        selectionPage = pager.settledPage
        selectionOwner = work.key
    }
    LaunchedEffect(work.report, work.running) {
        if (!work.running && work.report != null && pending.isEmpty()) isSelecting = false
    }
    ResultEffect<CourseFilterSelection> { selection ->
        val previous = viewModel.uiState.value as? CourseSelectionUiState.Success
        if (previous != null && (previous.category != selection.category || previous.filters != selection.filters)) {
            viewModel.search(query, selection.category, selection.filters)
            searchGridState.requestScrollToItem(0)
            if (backStack.lastOrNull().studyDetailRoute() != null) backStack.removeLastOrNull()
        }
    }
    LaunchedEffect(operationNotice, backStack.lastOrNull(), refreshing) {
        operationNotice?.let {
            if (backStack.lastOrNull() == CourseSelectionRoute && !refreshing) {
                viewModel.consumeOperationNotice()
                viewModel.refresh()

            }
        }
    }
    LaunchedEffect(work.key) {
        while (true) { workspace.refreshExpiry(); delay(60_000) }
    }
    LaunchedEffect(work.preferenceMode, work.key) {
        val value = viewModel.uiState.value as? CourseSelectionUiState.Success
        if (work.preferenceMode && value != null && value.filters.filterFull) viewModel.search(query, filters = value.filters.copy(filterFull = false))
    }
    LaunchedEffect(query) {
        delay(400)
        val success = state as? CourseSelectionUiState.Success
        if (success != null && success.query != query) viewModel.search(query)
    }
    LaunchedEffect(pager.settledPage, current?.searchResults?.size, current?.hasMore, current?.isSearching, current?.appendError, refreshing) {
        if (pager.settledPage != CourseSelectionTab.Select.ordinal) return@LaunchedEffect
        // 基础课/选修课接口不接受关键词，当前页没有匹配项时继续取下一页。
        if (current != null && current.category in listOf(CourseSelectCategory.Basic, CourseSelectCategory.Optional) &&
            query.isNotBlank() && current.searchResults.none { it.name.contains(query.trim(), true) || it.teacher.contains(query.trim(), true) } &&
            current.hasMore && !current.isSearching && !current.appendError && !refreshing) {
            viewModel.search(query, append = true)
            return@LaunchedEffect
        }
        snapshotFlow { searchGridState.layoutInfo.let { (it.visibleItemsInfo.lastOrNull()?.index ?: -1) to it.totalItemsCount } }
            .collect { (last, total) ->
                val value = viewModel.uiState.value as? CourseSelectionUiState.Success
                if (total > 0 && last >= total - 5 && value?.hasMore == true && !value.isSearching && !value.appendError && !refreshing)
                    viewModel.search(query, append = true)
            }
    }
    var settledTab by rememberSaveable { mutableIntStateOf(pager.settledPage) }
    LaunchedEffect(pager.settledPage) {
        if (settledTab != pager.settledPage && backStack.lastOrNull().studyDetailRoute() != null) backStack.removeLastOrNull()
        settledTab = pager.settledPage
    }
    LaunchedEffect(pager.settledPage, current?.overview?.entrance?.id) { if (pager.settledPage == CourseSelectionTab.DropLogs.ordinal) viewModel.loadLogs() }
    LaunchedEffect(pager.settledPage, current?.overview?.entrance?.id, current?.timetable == null) {
        if (pager.settledPage == CourseSelectionTab.Timetable.ordinal) viewModel.loadTimetable()
    }
    LaunchedEffect(current?.notice) {
        if (current?.notice != null) {
            viewModel.consumeNotice()
            feedbackScope.launch { snackbar.showSnackbar("课程加载失败，请重试。") }
        }
    }
    Scaffold(topBar = {
        StudyTopBar {
            PomTopAppBar(title = { Text(if (isSelecting) if (work.running) "正在提交 ${work.completed}/${work.total}" else "已选 ${pending.size} 门" else "选课系统") }, shadowElevation = 0.dp,
                navigationIcon = { BackIconButton(onClick = { if (isSelecting) finishSelection() else backStack.removeLastOrNull() }) },
                actions = {
                    if (isSelecting) {
                        TextButton(enabled = !work.running && batchItems.isNotEmpty(), onClick = {
                            batchItems.forEach { item ->
                                if (allSelected || pending.none { it.id == item.id }) workspace.togglePending(item, drop)
                            }
                        }) { Text(if (allSelected) "取消全选" else "全选") }
                        IconButton(enabled = pending.isNotEmpty() && !work.running && !work.expired, onClick = {
                            if (drop) backStack.add(CourseOperationRoute(CourseOperation.Drop, "batch", "${pending.size} 门课程", items = pending, ownerKey = work.key))
                            else if (work.preferenceMode) backStack.add(CourseBasketRoute(false, work.key))
                            else submitCourseBasket(workspace, pending, feedbackScope, snackbar)
                        }) {
                            Icon(if (drop) Icons.Rounded.Delete else Icons.Rounded.Done,
                                if (drop) "退选选中课程" else if (work.preferenceMode) "设置选中课程志愿" else "选中课程选课",
                                tint = if (drop && pending.isNotEmpty()) MaterialTheme.colorScheme.error else LocalContentColor.current)
                        }
                    } else {
                    if (batchEnabled) IconButton(enabled = batchItems.isNotEmpty() && !work.running, onClick = { beginSelection() }) {
                        Icon(Icons.Rounded.Checklist, if (drop) "多选退课" else "多选选课")
                    }
                    current?.let { value ->
                        IconButton(onClick = {
                            backStack.add(CourseInstructionsRoute(StudySection("", fields(
                                "批次" to value.overview.entrance.name,
                                "学期" to value.overview.entrance.semester.toString(),
                                "开放时间" to "${value.overview.entrance.startTime} 至 ${value.overview.entrance.endTime}",
                                "注意事项" to value.overview.credits.note))))
                        }) { Icon(Icons.Outlined.Info, "选课说明") }
                    }
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, "刷新") }
                    }
                })
            if (current != null) {
                StudyTabs(CourseSelectionTab.entries.map { it.label }, pager.currentPage, scrollable = true) { index -> fallbackScope.launch { pager.animateScrollToPage(index) } }
            }
        }
    }, floatingActionButton = { if (tab != CourseSelectionTab.Timetable) BackToTopButton(gridState) }) { padding ->
        PomPullToRefreshBox(refreshing && state is CourseSelectionUiState.Success, viewModel::refresh, contentPadding = padding,
            enabled = state is CourseSelectionUiState.Success && tab != CourseSelectionTab.Timetable) {
            when (val value = state) {
                CourseSelectionUiState.Loading -> LoadingContent(contentPadding = padding)
                CourseSelectionUiState.NotLoggedIn -> NotLoggedInContent(onLogin = { backStack.add(LoginRoute) }, contentPadding = padding)
                is CourseSelectionUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorContent(value.throwable, Modifier.weight(1f)); TextButton(onClick = viewModel::refresh) { Text("重试") }
                }
                is CourseSelectionUiState.Success -> HorizontalPager(pager, Modifier.fillMaxSize().padding(padding.withoutBottom()),
                    userScrollEnabled = pager.settledPage != CourseSelectionTab.Timetable.ordinal, verticalAlignment = Alignment.Top) { page ->
                    val tab = CourseSelectionTab.entries[page]
                    val gridState = when (tab) { CourseSelectionTab.Select -> searchGridState; CourseSelectionTab.Selected -> selectedGridState; else -> favoriteGridState }
                    Column(Modifier.fillMaxSize()) {
                        if (tab == CourseSelectionTab.Select) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StudySearch(query, { query = it; searchGridState.requestScrollToItem(0) }, "搜索课程或教师", Modifier.weight(1f))
                                    FilledTonalIconButton(onClick = { backStack.add(CourseFiltersRoute(value.category, value.filters)) }) {
                                        Icon(Icons.Rounded.Tune, "筛选课程")
                                    }
                                }
                                Text("${value.category.label} · ${value.filters.summary(value.category)}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (tab == CourseSelectionTab.Favorites) StudySearch(favoriteQuery, { favoriteQuery = it; favoriteGridState.requestScrollToItem(0) },
                            "搜索收藏的课程、教师或编号", Modifier.padding(16.dp))
                        if (tab == CourseSelectionTab.Selected) StudySearch(dropQuery, { dropQuery = it; selectedGridState.requestScrollToItem(0) },
                            "搜索已选课程、教师或编号", Modifier.padding(16.dp))
                        if (tab == CourseSelectionTab.Timetable) {
                            CourseSelectionTimetableContent(value, work, padding.calculateBottomPadding(), viewModel::loadTimetable)
                        } else if (tab == CourseSelectionTab.Credits) {
                            CourseCreditsContent(value, infoState, padding.calculateBottomPadding())
                        } else if (tab == CourseSelectionTab.DropLogs) {
                            CourseDropLogsContent(value, logState, padding.calculateBottomPadding(), onRetry = viewModel::loadLogs)
                        } else {
                            val candidates = if (tab == CourseSelectionTab.Favorites) work.favorites.filter { it.matches(favoriteQuery) }.map { it.course to CourseSelectCategory.valueOf(it.category) }
                            else value.searchResults.filter { value.category !in listOf(CourseSelectCategory.Basic, CourseSelectCategory.Optional) || query.isBlank() ||
                                it.name.contains(query.trim(), true) || it.teacher.contains(query.trim(), true) }.map { it to value.category }
                            val records = (if (tab == CourseSelectionTab.Selected) value.overview.selectedCourses.filter { it.matches(dropQuery) }.map { it.record() } else candidates.map { (course, category) ->
                                course.copy(isSelected = work.selectionOverrides[course.id] ?: (course.isSelected || course.id in work.selectedIds || work.submittedPreferences.containsKey("${course.courseId}:${course.id}"))).record(category, work.preferenceMode)
                            }).map { record -> record.copy(detail = record.detail.copy(action = if (work.expired) null else record.detail.action?.copy(ownerKey = work.key))) }
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                if (records.isEmpty() && value.isSearching && tab == CourseSelectionTab.Select) LoadingContent()
                                else if (records.isEmpty() && !(tab == CourseSelectionTab.Favorites && work.archived.isNotEmpty())) EmptyContent(message = when {
                                    tab == CourseSelectionTab.Selected -> if (dropQuery.isBlank()) "还没有已选课程" else "没有符合条件的已选课程"
                                    tab == CourseSelectionTab.Favorites -> if (favoriteQuery.isBlank()) "还没有收藏课程" else "没有符合条件的收藏课程"
                                    else -> "没有符合条件的课程"
                                })
                                else LazyVerticalGrid(GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                                    Modifier.widthIn(max = 1100.dp).fillMaxSize(), state = gridState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (tab == CourseSelectionTab.Favorites) item(span = { GridItemSpan(maxLineSpan) }) { FavoriteScopeHeader(workspace) }
                                    itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                                        val source = if (tab == CourseSelectionTab.Selected) null else candidates[index]
                                        SelectionCourseCard(record, index, workspace, enabled = !refreshing && !work.running,
                                            favoriteSource = source,
                                            selecting = isSelecting && batchEnabled,
                                            canSelect = tab == CourseSelectionTab.Favorites && record.detail.action?.drop == false || tab == CourseSelectionTab.Selected && record.detail.action != null,
                                            onStartSelection = { record.detail.cartItem()?.let { beginSelection(it) } },
                                            onToggleSelection = { record.detail.cartItem()?.let { workspace.togglePending(it, drop) } },
                                            onDetail = { backStack.openAdaptiveStudyDetail(record.detail) },
                                            onAction = { performCourseAction(record.detail, workspace, backStack, feedbackScope, snackbar) })
                                    }
                                    if (tab == CourseSelectionTab.Favorites && work.archived.isNotEmpty()) {
                                        item(span = { GridItemSpan(maxLineSpan) }) { Text("历史收藏", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        items(work.archived, key = { "${it.scope.key}:${it.favorite.course.id}" }) { ArchivedFavoriteCard(it) }
                                    }
                                    if (tab == CourseSelectionTab.Select && value.hasMore) item(span = { GridItemSpan(maxLineSpan) }) {
                                        if (value.appendError) TextButton(onClick = { viewModel.search(query, append = true) }, modifier = Modifier.fillMaxWidth()) { Text("加载失败，重试") }
                                        else if (value.isSearching) Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { PomLoadingIndicator(size = InlineLoadingIndicatorSize) }
                                        else Spacer(Modifier.height(1.dp))
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

@Composable
internal fun CourseInstructionsSheet(route: CourseInstructionsRoute) {
    val backStack = LocalBackStack.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("选课说明", style = MaterialTheme.typography.titleLarge)
        val workspace = org.koin.compose.koinInject<CourseSelectionWorkspace>()
        val work by workspace.state.collectAsState()
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("填报志愿", style = MaterialTheme.typography.titleMedium)
                    Text("抽签轮次开启，先选先得轮次关闭。提交志愿后由学校统一处理结果。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
                Switch(work.preferenceMode, { checked -> scope.launch {
                    try { workspace.setPreferenceMode(checked) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { workspace.showError(failure.message ?: "保存失败，请重试。") }
                } }, enabled = !work.running && work.key.isNotEmpty())
            }
        }
        work.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        StudySectionContent(route.section)
    }
}

@Composable
private fun SelectionCourseCard(record: StudyRecord, index: Int, workspace: CourseSelectionWorkspace, enabled: Boolean,
    favoriteSource: Pair<SelectableCourse, CourseSelectCategory>?, selecting: Boolean, canSelect: Boolean,
    onStartSelection: () -> Unit, onToggleSelection: () -> Unit, onDetail: () -> Unit, onAction: () -> Unit) {
    val action = record.detail.action
    val selected = LocalBackStack.current.lastOrNull().studyDetailRoute()?.id == record.id
    val work by workspace.state.collectAsState()
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val item = record.detail.cartItem()
    val queued = item?.let { c -> (if (action?.drop == true) work.pendingDrop else work.pendingSelect).any { it.id == c.id } } == true
    ExpressiveActionCard(onClick = { if (selecting) { if (canSelect && enabled) onToggleSelection() } else onDetail() },
        onLongClick = if (canSelect && enabled) { { if (selecting) onToggleSelection() else onStartSelection() } } else null,
        modifier = Modifier.semantics { if (selecting) this.selected = queued }, selected = if (selecting) queued else selected) { pressed ->
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpressiveIconBadge(Icons.Rounded.School, index, pressed)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(courseNameText(record.title), style = MaterialTheme.typography.titleMedium)
                    Text(record.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selecting) Checkbox(checked = queued, onCheckedChange = if (canSelect) { { onToggleSelection() } } else null, enabled = canSelect && enabled)
                else if (action != null && LocalDensity.current.fontScale <= 1.3f) {
                    FilledTonalButton(onClick = onAction, enabled = enabled, colors = courseActionColors(action.drop), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(if (action.drop) "退课" else if (work.preferenceMode) "提交志愿" else "选课")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (record.supporting.isNotBlank()) Text(record.supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (record.value.isNotBlank()) Text(record.value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (!selecting) favoriteSource?.let { (course, category) ->
                    IconToggleButton(checked = work.favorites.any { it.course.id == course.id }, enabled = enabled,
                        colors = IconButtonDefaults.iconToggleButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContentColor = MaterialTheme.colorScheme.tertiary), onCheckedChange = {
                        scope.launch {
                            try { workspace.toggleFavorite(course, category.name) }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (failure: Exception) { snackbar.showSnackbar(failure.message ?: "收藏失败，请重试。") }
                        }
                    }) { Icon(if (work.favorites.any { it.course.id == course.id }) Icons.Rounded.Star else Icons.Outlined.StarBorder, "收藏 ${course.name}") }
                }
            }
            if (!selecting && action != null && LocalDensity.current.fontScale > 1.3f) FilledTonalButton(onClick = onAction, enabled = enabled, colors = courseActionColors(action.drop)) { Text(if (action.drop) "退课" else if (work.preferenceMode) "提交志愿" else "选课") }
        }
    }
}

private fun FavoriteCourse.matches(query: String) = query.isBlank() || listOf(course.name, course.teacher, course.courseId).any { it.contains(query.trim(), true) }
private fun SelectedCourse.matches(query: String) = query.isBlank() || listOf(name, teacher, courseId).any { it.contains(query.trim(), true) }

internal fun SelectableCourse.record(category: CourseSelectCategory, preferenceMode: Boolean = false): StudyRecord {
    val key = "select:$id"
    val status = when { isSelected -> if (preferenceMode) "已提交志愿" else "已选"; !isOpenForSelection -> "暂未开放"; remaining <= 0 -> "名额已满"; else -> "余 $remaining / $capacity" }
    return StudyRecord(key, name, "$teacher · ${credit(credits)} 学分", schedule,
        value = status, detail = StudyDetailRoute(key, name, "$teacher · ${credit(credits)} 学分",
            listOf(StudySection("上课安排", fields("上课时间" to schedule, "教室" to classroom,
                "校区" to when (campusId) { "1" -> "佛山校区"; "2" -> "广州校区"; else -> campusId }, "选课状态" to status,
                "冲突说明" to conflictDescription)), StudySection("课程信息", fields("课程编号" to courseId,
                "课程分类" to category.label, "开课学院" to department, "总学时" to totalHours.toString(), "备注" to note))),
            action = if (isSelected || (isOpenForSelection && !isDeleted && (preferenceMode || remaining > 0))) StudyAction(id, name, category.name, drop = isSelected, courseCode = courseId, credits = credits, teacher = teacher) else null))
}
internal fun SelectedCourse.record(): StudyRecord {
    val key = "selected:$id"
    return StudyRecord(key, name, "$teacher · ${credit(credits)} 学分", timeString.orEmpty(), detail = StudyDetailRoute(key, name, "$teacher · ${credit(credits)} 学分",
        listOf(StudySection("已选课程", fields("课程编号" to courseId, "上课时间" to timeString, "教室" to classroom, "课程属性" to attribute))),
        action = StudyAction(id, name, CourseSelectCategory.General.name, drop = true, courseCode = courseId, credits = credits, teacher = teacher)))
}

@Composable
internal fun CourseOperationDialog(route: CourseOperationRoute) {
    val restoredStack = LocalBackStack.current
    // 旧版本保存的选课确认页或缺少账号作用域的操作不能作为退课执行。
    if (route.operation != CourseOperation.Drop || route.ownerKey.isEmpty()) {
        LaunchedEffect(route) { if (restoredStack.lastOrNull() == route) restoredStack.finishCourseOperation() }
        return
    }
    val workspace = org.koin.compose.koinInject<CourseSelectionWorkspace>()
    val work by workspace.state.collectAsState()
    val backStack = LocalBackStack.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    var error by remember { mutableStateOf<String?>(null) }
    val items = route.items.ifEmpty { listOf(CourseCartItem(route.courseId, route.courseId, route.courseName, "", 0.0, route.category,
        StudyDetailRoute(route.courseId, route.courseName, route.summary))) }
    AlertDialog(onDismissRequest = { if (!work.running) backStack.removeLastOrNull() },
        properties = DialogProperties(dismissOnBackPress = !work.running, dismissOnClickOutside = !work.running),
        title = { Text(if (items.size > 1) "确认退选 ${items.size} 门课程" else "确认退课") },
        text = {
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("退课后名额会释放，可能无法重新选回。请确认后再退课。", style = MaterialTheme.typography.bodyMedium)
                items.forEach { item ->
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(courseNameText(item.name), style = MaterialTheme.typography.titleSmall)
                            if (item.detail.summary.isNotBlank()) Text(item.detail.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (work.running) Text("正在退课 ${work.completed} / ${work.total}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                enabled = !work.running && work.key.isNotEmpty(), onClick = {
                    error = null
                    scope.launch {
                        try {
                            val report = workspace.submit(items, true, route.ownerKey.ifEmpty { work.key }) ?: return@launch
                            if (report.successCount > 0) {
                                backStack.finishCourseOperation()
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(report.message)
                            } else error = report.results.joinToString("\n") { "${it.item.name}：${it.message}" }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (failure: Exception) { error = failure.message ?: "退课失败，请重试。" }
                    }
                }) {
                if (work.running) { PomLoadingIndicator(size = InlineLoadingIndicatorSize); Spacer(Modifier.width(8.dp)) }
                Text(if (work.running) "处理中" else "确认退课")
            }
        }, dismissButton = {
            TextButton(onClick = { backStack.removeLastOrNull() }, enabled = !work.running) { Text("保留课程") }
        })
}
