package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.clickable
import kotlin.time.Clock
import kotlin.math.roundToInt
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object TimetableRoute : NavKey

/** 手机和桌面共用课表；日视图按天分页，浏览状态只保留在当前会话。 */
@Composable
fun TimetableScreen(toolbarExpanded: Boolean, onToolbarExpandedChange: (Boolean) -> Unit,
    viewModel: TimetableViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val state by viewModel.uiState.collectAsState()
    val week by viewModel.week.collectAsState()
    val day by viewModel.selectedDay.collectAsState()
    val campus by viewModel.campus.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val showOtherWeeks by viewModel.showOtherWeeks.collectAsState()
    val customizations by viewModel.customizations.collectAsState()
    val today = rememberCurrentDate()
    LaunchedEffect(today) { viewModel.updateDate(today) }
    var dayView by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val useDayView = dayView ?: (density.fontScale > 1.5f)
    val weekPager = rememberPagerState(initialPage = week) { TIMETABLE_MAX_WEEK }
    val dayPager = rememberPagerState(initialPage = week * 7 + day) { TIMETABLE_MAX_WEEK * 7 }
    val pager = if (useDayView) dayPager else weekPager
    val displayedWeek = if (useDayView) pager.currentPage / 7 else pager.currentPage
    val displayedDay = if (useDayView) pager.currentPage % 7 else day
    val success = state as? TimetableUiState.Success
    var previousMode by remember { mutableStateOf<Boolean?>(null) }
    var navigationTarget by remember { mutableStateOf<Int?>(null) }

    fun dateAt(index: Int) = timetableDateForDay(index, currentWeek, today)
    fun selectDate(date: LocalDate) {
        val index = dayIndexForDate(date, currentWeek, today).coerceIn(0, TIMETABLE_MAX_WEEK * 7 - 1)
        viewModel.selectDate(dateAt(index))
        viewModel.setWeek(index / 7)
    }
    fun selectWeek(target: Int) {
        val selectedWeek = target.coerceIn(0, TIMETABLE_MAX_WEEK - 1)
        selectDate(weekDates(selectedWeek, today, currentWeek)[displayedDay])
    }
    ResultEffect<TimetableWeekSelection> { selectWeek(it.week) }
    ResultEffect<TimetableDateSelection> { selectDate(it.date) }

    LaunchedEffect(week, day, useDayView, currentWeek, today, success != null) {
        if (success == null) return@LaunchedEffect
        val target = if (useDayView) week * 7 + day else week
        navigationTarget = target
        try {
            if (previousMode != useDayView) pager.scrollToPage(target)
            else if (pager.currentPage != target) pager.animateScrollToPage(target)
            previousMode = useDayView
        } finally { navigationTarget = null }
    }
    LaunchedEffect(pager, useDayView, currentWeek, today, success != null) {
        if (success == null) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.drop(1).distinctUntilChanged().collect { page ->
            if (navigationTarget != null && page != navigationTarget) return@collect
            val settledWeek = if (useDayView) page / 7 else page
            if (useDayView) viewModel.selectDate(dateAt(page))
            if (settledWeek != viewModel.week.value) viewModel.setWeek(settledWeek)
            val detail = backStack.lastOrNull() as? TimetableCourseRoute
            if (detail != null && detail.week != settledWeek) backStack[backStack.lastIndex] = detail.copy(week = settledWeek)
        }
    }
    fun open(courses: List<TimetableCourse>, row: Int, column: Int, page: Int) {
        val timetable = success?.timetable ?: return
        val route = timetableCourseRoute(timetable, courses, page, row, column)
        val previous = backStack.lastOrNull() as? TimetableCourseRoute
        if (previous != null) {
            backStack.removeLastOrNull()
            if (previous.lessons.map { it.key } == route.lessons.map { it.key }) return
        }
        backStack.add(route)
    }
    fun pickDate() {
        backStack.add(TimetableDateRoute(weekDates(displayedWeek, today, currentWeek)[displayedDay].utcMillis(),
            weekDates(0, today, currentWeek).first().utcMillis(),
            weekDates(TIMETABLE_MAX_WEEK - 1, today, currentWeek).last().utcMillis()))
    }
    Scaffold(topBar = {
        TimetableHeader(displayedWeek, success?.timetable, useDayView, showOtherWeeks,
            canGoPrevious = pager.currentPage > 0, canGoNext = pager.currentPage < pager.pageCount - 1,
            onPrevious = { if (useDayView) selectDate(dateAt(dayPager.currentPage - 1)) else selectWeek(displayedWeek - 1) },
            onNext = { if (useDayView) selectDate(dateAt(dayPager.currentPage + 1)) else selectWeek(displayedWeek + 1) },
            showReturn = if (useDayView) dateAt(dayPager.currentPage) != today else displayedWeek != currentWeek,
            onReturnToday = viewModel::returnToToday,
            onDayView = { dayView = it },
            onPickWeek = { backStack.add(TimetableWeekRoute(displayedWeek)) },
            onShowOtherWeeks = viewModel::setShowOtherWeeks,
            onNotes = { success?.let { backStack.add(TimetableNotesRoute(it.timetable.note.orEmpty())) } },
            onRefresh = viewModel::refresh)
    }) { padding ->
        val horizontal = PaddingValues(start = padding.calculateStartPadding(layoutDirection), end = padding.calculateEndPadding(layoutDirection))
        val contentModifier = Modifier.fillMaxSize().padding(horizontal).padding(top = padding.calculateTopPadding())
        when (val value = state) {
            TimetableUiState.Loading -> LoadingContent(contentPadding = padding)
            TimetableUiState.NotLoggedIn -> NotLoggedInContent(onLogin = { backStack.add(LoginRoute) }, contentPadding = padding)
            is TimetableUiState.Error -> Column(contentModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorContent(value.throwable, Modifier.weight(1f)); TextButton(onClick = viewModel::refresh) { Text("重试") }
            }
            is TimetableUiState.Success -> BoxWithConstraints(contentModifier) {
                val wide = maxWidth >= 700.dp
                val gridHeight = maxHeight
                val sidePadding = if (wide) 16.dp else 8.dp
                val bottom = padding.calculateBottomPadding() + if (wide) 16.dp else with(FloatingToolbarDefaults) { ContainerSize + ScreenOffset }
                if (useDayView) Column(Modifier.fillMaxSize()) {
                    TimetableDayDates(dayPager.currentPage, currentWeek, today, Modifier.padding(horizontal = sidePadding),
                        onPickDate = ::pickDate, onSelect = { selectDate(dateAt(it)) })
                    HorizontalPager(dayPager, Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
                        val pageWeek = page / 7
                        val pageDay = page % 7
                        val display = remember(value.timetable, pageWeek, showOtherWeeks) { TimetableDisplay.of(value.timetable.courses, pageWeek, showOtherWeeks) }
                        val courses = display.courses.values.flatMap { it[DayOfWeek.entries[pageDay]].orEmpty() }.distinct()
                            .sortedWith(compareBy<TimetableCourse> { !it.isInWeek(pageWeek) }.thenBy { it.sections.minOrNull() ?: 0 })
                        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            TimetableDay(courses, value.timetable, customizations, pageWeek, bottom, toolbarExpanded, onToolbarExpandedChange) {
                                open(listOf(it), ((it.sections.minOrNull() ?: 1) - 1) / 2, pageDay, pageWeek)
                            }
                        }
                    }
                } else HorizontalPager(weekPager, Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
                    val display = remember(value.timetable, page, showOtherWeeks) { TimetableDisplay.of(value.timetable.courses, page, showOtherWeeks) }
                    Column(Modifier.fillMaxSize().padding(horizontal = sidePadding)) {
                        WeekdayHeader(page, currentWeek, today, onPickDate = ::pickDate,
                            onSelect = { index ->
                                selectDate(weekDates(page, today, currentWeek)[index])
                                dayView = true
                            })
                        TimetableWeek(display, value.timetable, customizations,
                            ((gridHeight - 64.dp - bottom - 48.dp) / TIMETABLE_ROW_COUNT).coerceAtLeast((if (showOtherWeeks) 96.dp else 80.dp) * density.fontScale.coerceAtLeast(1f)), bottom, wide,
                            toolbarExpanded, onToolbarExpandedChange, page, campus,
                            onClick = { courses, row, column -> open(courses, row, column, page) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableHeader(week: Int, timetable: Timetable?, dayView: Boolean, showOtherWeeks: Boolean,
    canGoPrevious: Boolean, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit,
    showReturn: Boolean, onReturnToday: () -> Unit, onDayView: (Boolean) -> Unit, onPickWeek: () -> Unit,
    onShowOtherWeeks: (Boolean) -> Unit, onNotes: () -> Unit, onRefresh: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val width = with(density) { window.width.toDp() }
    val height = with(density) { window.height.toDp() }
    val compact = density.fontScale <= 1.3f || height < 480.dp
    val compactActionSize = if (width < 340.dp) 32.dp else 36.dp
    val options: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menu = true }, modifier = if (compact) Modifier.size(compactActionSize) else Modifier) {
                Icon(Icons.Rounded.MoreVert, "课表选项")
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                if (showReturn) DropdownMenuItem(text = { Text(if (dayView) "回到今天" else "回到本周") },
                    leadingIcon = { Icon(Icons.Rounded.Today, null) }, onClick = { menu = false; onReturnToday() })
                DropdownMenuItem(text = { Text("刷新课表") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                    onClick = { menu = false; onRefresh() })
                DropdownMenuItem(text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("显示非本周课程")
                        Text("淡色虚线为非本周，叠层可点开查看", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }, trailingIcon = { Switch(showOtherWeeks, onCheckedChange = null) },
                    onClick = { onShowOtherWeeks(!showOtherWeeks) })
            }
        }
    }
    if (compact) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(TopAppBarDefaults.windowInsets).height(if (height < 480.dp) 48.dp else 56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val arrowSize = if (width < 340.dp) 32.dp else 36.dp
            IconButton(onClick = onPrevious, enabled = canGoPrevious, modifier = Modifier.size(arrowSize)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, if (dayView) "上一天" else "上一周")
            }
            Row(Modifier.width(if (width >= 600.dp) 84.dp * density.fontScale.coerceAtLeast(1f) else 84.dp).height(48.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onPickWeek),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("第 $week 周", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Icon(Icons.Rounded.ExpandMore, "选择周次", Modifier.size(16.dp))
            }
            IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(arrowSize)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, if (dayView) "下一天" else "下一周")
            }
            Spacer(Modifier.weight(1f))
            ViewModeToggle(dayView, onDayView, compact = true)
            IconButton(onClick = onNotes, enabled = timetable != null, modifier = Modifier.size(compactActionSize)) { Icon(Icons.Outlined.Info, "课表备注") }
            options()
        }
        }
    } else Column {
        PomTopAppBar(title = {
            TextButton(onClick = onPickWeek, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("第 $week 周", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.ExpandMore, null)
            }
        }, actions = {
            if (showReturn) TextButton(onClick = onReturnToday) { Text(if (dayView) "今天" else "本周") }
            IconButton(onClick = onNotes, enabled = timetable != null) { Icon(Icons.Outlined.Info, "课表备注") }
            options()
        })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onPrevious, enabled = canGoPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, if (dayView) "上一天" else "上一周")
            }
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, if (dayView) "下一天" else "下一周")
            }
            Spacer(Modifier.weight(1f))
            ViewModeToggle(dayView, onDayView)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ViewModeToggle(dayView: Boolean, onChange: (Boolean) -> Unit, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = ButtonDefaults.shape
    val textHeight = with(LocalDensity.current) { MaterialTheme.typography.labelLarge.lineHeight.toDp() }
    val height = (textHeight + 8.dp).coerceAtLeast(if (compact) 28.dp else 36.dp)
    val width = if (compact) 40.dp else 52.dp
    val offset by animateDpAsState(if (dayView) width else 0.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "Timetable mode indicator")
    Surface(shape = shape, color = colors.surfaceContainerHigh) {
        Box(Modifier.padding(2.dp)) {
            Surface(Modifier.offset(x = offset).width(width).height(height), shape = shape,
                color = colors.primary) {}
            Row(Modifier.selectableGroup()) {
                listOf(false to "周", true to "日").forEach { (value, label) ->
                    val selected = dayView == value
                    val contentColor by animateColorAsState(if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "Timetable mode label")
                    Surface(selected = selected, onClick = { onChange(value) },
                        modifier = Modifier.width(width).height(height), shape = shape,
                        color = Color.Transparent, contentColor = contentColor) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableDayDates(selectedIndex: Int, currentWeek: Int, today: LocalDate, modifier: Modifier = Modifier,
    onPickDate: () -> Unit, onSelect: (Int) -> Unit) {
    val date = timetableDateForDay(selectedIndex, currentWeek, today)
    val density = LocalDensity.current
    val shortWindow = with(density) { LocalWindowInfo.current.containerSize.height.toDp() < 480.dp } && density.fontScale <= 1.3f
    Row(modifier.fillMaxWidth().padding(vertical = if (shortWindow) 2.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MonthButton(date.month.number, shortWindow, onPickDate)
        BoxWithConstraints(Modifier.weight(1f)) {
            val cellWidth = ((maxWidth - 24.dp) / 7).coerceAtLeast(40.dp * density.fontScale.coerceAtLeast(1f))
            val visibleCount = ((maxWidth.value + 4f) / (cellWidth.value + 4f)).roundToInt().coerceAtLeast(1)
            val firstVisible = (selectedIndex - visibleCount / 2).coerceIn(0, TIMETABLE_MAX_WEEK * 7 - visibleCount)
            val list = rememberLazyListState(initialFirstVisibleItemIndex = firstVisible)
            LaunchedEffect(firstVisible) { list.animateScrollToItem(firstVisible) }
            LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(TIMETABLE_MAX_WEEK * 7, key = { timetableDateForDay(it, currentWeek, today).toString() }) { index ->
                    Box(Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                        TimetableDateChip(timetableDateForDay(index, currentWeek, today), index == selectedIndex, today, shortWindow,
                            Modifier.width(40.dp * density.fontScale.coerceAtLeast(1f)), showTodayWhenUnselected = true) { onSelect(index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(week: Int, currentWeek: Int, today: LocalDate, onPickDate: () -> Unit, onSelect: (Int) -> Unit) {
    val dates = remember(week, currentWeek, today) { weekDates(week, today, currentWeek) }
    val density = LocalDensity.current
    val shortWindow = with(density) { LocalWindowInfo.current.containerSize.height.toDp() < 480.dp } && density.fontScale <= 1.3f
    Row(Modifier.fillMaxWidth().padding(vertical = if (shortWindow) 2.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MonthButton(dates.first().month.number, shortWindow, onPickDate)
        dates.forEachIndexed { index, date ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TimetableDateChip(date, date == today, today, shortWindow,
                    Modifier.width(40.dp)) { onSelect(index) }
            }
        }
    }
}

@Composable
private fun MonthButton(month: Int, compact: Boolean, onClick: () -> Unit) {
    Box(Modifier.width(44.dp).heightIn(min = if (compact) 36.dp else 44.dp).clip(MaterialTheme.shapes.small)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "选择日期", onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text("${month}月", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun TimetableDateChip(date: LocalDate, selected: Boolean, today: LocalDate, compact: Boolean,
    modifier: Modifier = Modifier, showTodayWhenUnselected: Boolean = false, onClick: () -> Unit) {
    val isToday = date == today
    val colors = MaterialTheme.colorScheme
    val labelStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    val dateStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val height = with(LocalDensity.current) { labelStyle.lineHeight.toDp() + dateStyle.lineHeight.toDp() } + if (compact) 4.dp else 8.dp
    Surface(selected = selected, onClick = onClick, modifier = modifier.height(height), shape = MaterialTheme.shapes.medium,
        color = when { selected && isToday -> colors.primary; selected -> colors.primaryContainer; else -> Color.Transparent },
        contentColor = when { selected && isToday -> colors.onPrimary; selected -> colors.onPrimaryContainer; else -> colors.onSurfaceVariant },
        border = if (isToday && !selected) BorderStroke(1.dp, colors.primary) else null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            val label = if (showTodayWhenUnselected && isToday && !selected) "今天" else "一二三四五六日"[date.dayOfWeek.ordinal].toString()
            Text(label, style = labelStyle, maxLines = 1)
            Text(date.day.toString(), style = dateStyle, maxLines = 1)
        }
    }
}

/** 节次栏保持可读宽度，其余空间由七天等分；不随屏幕变窄而裁掉 11–12。 */
private val TimetableCells = object : GridCells {
    override fun androidx.compose.ui.unit.Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val usable = (availableSize - spacing * 7).coerceAtLeast(0)
        val gutter = 44.dp.roundToPx().coerceAtMost(usable)
        val remaining = usable - gutter
        return listOf(gutter) + List(7) { remaining / 7 + if (it < remaining % 7) 1 else 0 }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableWeek(display: TimetableDisplay, timetable: Timetable,
    customizations: Map<String, TimetableCourseCustomization>, rowHeight: androidx.compose.ui.unit.Dp,
    bottom: androidx.compose.ui.unit.Dp, wide: Boolean, toolbarExpanded: Boolean, onToolbarExpandedChange: (Boolean) -> Unit, week: Int, campus: Campus,
    onClick: (List<TimetableCourse>, Int, Int) -> Unit) {
    val grid = rememberLazyGridState()
    val rowTimes = remember(campus) { sectionTimeRanges(campus).chunked(2).map { it.first().first to it.last().second } }
    val selection = (LocalBackStack.current.lastOrNull() as? TimetableCourseRoute)?.lessons.orEmpty().map { it.key }.toSet()
    LazyVerticalGrid(TimetableCells, Modifier.fillMaxSize().floatingToolbarVerticalNestedScroll(toolbarExpanded,
        onExpand = { onToolbarExpandedChange(true) }, onCollapse = { onToolbarExpandedChange(false) }),
        state = grid, contentPadding = PaddingValues(top = 6.dp, bottom = bottom + 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(TIMETABLE_ROW_COUNT) { row ->
            if (row > 0) item(key = "divider:$row", span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            }
            item(key = "time:$row") {
                Column(Modifier.fillMaxWidth().height(rowHeight).padding(start = 2.dp), verticalArrangement = Arrangement.Center) {
                    Text("${row * 2 + 1}–${row * 2 + 2}", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    rowTimes.getOrNull(row)?.let { (start, end) ->
                        Text(start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(7, key = { "slot:$row:$it" }) { day ->
                val courses = display.coursesAt(row, DayOfWeek.entries[day]).sortedBy { !it.isInWeek(week) }
                if (courses.isEmpty()) Spacer(Modifier.height(rowHeight))
                else {
                    val first = courses.first()
                    val key = first.lesson(timetable.ownerKey()).key
                    CourseCell(first, first.name, first.classroom, courses.size, rowHeight, wide, (LocalTimetableDetailPane.current && key in selection),
                        !first.isInWeek(week), customizations[key]) { onClick(courses, row, day) }
                }
            }
        }
    }
}

@Composable
private fun CourseCell(course: TimetableCourse, name: String, classroom: String?, count: Int, height: androidx.compose.ui.unit.Dp,
    wide: Boolean, selected: Boolean, otherWeek: Boolean, customization: TimetableCourseCustomization?, onClick: () -> Unit) {
    TimetableCourseCard(name, classroom, Modifier.fillMaxWidth().height(height), teacher = course.teacher,
        colorKey = course.name, wide = wide, selected = selected, otherWeek = otherWeek, count = count,
        nameOverride = customization?.name, classroomOverride = customization?.classroom, onClick = onClick)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableDay(courses: List<TimetableCourse>, timetable: Timetable,
    customizations: Map<String, TimetableCourseCustomization>, week: Int, bottom: androidx.compose.ui.unit.Dp,
    toolbarExpanded: Boolean, onToolbarExpandedChange: (Boolean) -> Unit, onClick: (TimetableCourse) -> Unit) {
    if (courses.isEmpty()) { EmptyContent(message = "这一天没有课程"); return }
    LazyColumn(Modifier.fillMaxSize().floatingToolbarVerticalNestedScroll(toolbarExpanded,
        onExpand = { onToolbarExpandedChange(true) }, onCollapse = { onToolbarExpandedChange(false) }),
        contentPadding = PaddingValues(top = 16.dp, bottom = bottom + 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(courses, key = { it.lesson(timetable.ownerKey()).key }) { course ->
            val customization = customizations[course.lesson(timetable.ownerKey()).key]
            val room = customization?.classroom ?: course.classroom
            ExpressiveActionCard(onClick = { onClick(course) }) { _ ->
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(course.sections.sorted().joinToString("、") + "节", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(courseNameText(course.name, customization?.name), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (!course.isInWeek(week)) Text("非本周", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        room?.takeIf { it.isNotBlank() }?.let { Text(classroomText(course.classroom.orEmpty(), customization?.classroom), style = MaterialTheme.typography.bodyLarge) }
                        course.teacher?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

/** 第 0 周以本周为基准，每加一周顺延七天。 */
internal fun weekDates(week: Int, today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date, currentWeek: Int = 0): List<LocalDate> {
    val monday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)).plus(DatePeriod(days = (week - currentWeek) * 7))
    return (0..6).map { monday.plus(DatePeriod(days = it)) }
}
