package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.cancel
import com.chow.pomegranate.confirm
import com.chow.pomegranate.course_drop_succeeded
import com.chow.pomegranate.course_operation_failed
import com.chow.pomegranate.course_search_hint
import com.chow.pomegranate.course_search_label
import com.chow.pomegranate.course_selected
import com.chow.pomegranate.course_select_succeeded
import com.chow.pomegranate.course_selection
import com.chow.pomegranate.course_search_failed
import com.chow.pomegranate.course_selection_load_failed
import com.chow.pomegranate.course_summary
import com.chow.pomegranate.credit_progress
import com.chow.pomegranate.confirm_drop_course
import com.chow.pomegranate.confirm_drop_course_message
import com.chow.pomegranate.confirm_select_course
import com.chow.pomegranate.confirm_select_course_message
import com.chow.pomegranate.drop_course
import com.chow.pomegranate.no_selected_courses
import com.chow.pomegranate.processing
import com.chow.pomegranate.refresh
import com.chow.pomegranate.search
import com.chow.pomegranate.select_course
import com.chow.pomegranate.selectable_course_summary
import com.chow.pomegranate.selectable_general_courses
import com.chow.pomegranate.selected_courses
import com.chow.pomegranate.repository.CourseSelectionOverview
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.SelectableCourse
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.InlineLoadingIndicatorSize
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomLoadingIndicator
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.datetime.LocalDateTime
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.component.NotLoggedInContent
import com.chow.pomegranate.ui.screen.login.LoginRoute
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog

/** 选课操作类型。 */
@Serializable
enum class CourseOperation { Select, Drop }

/** 选课操作确认弹窗路由。 */
@Serializable
data class CourseOperationRoute(
    val operation: CourseOperation,
    val courseId: String,
    val courseName: String,
) : androidx.navigation3.runtime.NavKey

/** 选课系统页。 */
@Composable
internal fun CourseSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CourseSelectionViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val state by viewModel.uiState.collectAsState()

    // 文案映射放在 composable 作用域：LaunchedEffect 里不能调 stringResource
    val searchFailedMessage = stringResource(Res.string.course_search_failed)

    LaunchedEffect(backStack.lastOrNull()) {
        if (backStack.lastOrNull() == CourseSelectionRoute && state !is CourseSelectionUiState.Loading) {
            viewModel.refresh()
        }
    }

    // 必须在 composable 作用域取值：LaunchedEffect 不是 @Composable，里面不能调 stringResource
    val loadFailedMessage = stringResource(Res.string.course_selection_load_failed)

    LaunchedEffect(state) {
        when (val current = state) {
            is CourseSelectionUiState.Error -> snackbarHostState.showSnackbar(
                current.throwable.message ?: loadFailedMessage,
            )
            is CourseSelectionUiState.Success -> current.notice?.let { notice ->
                snackbarHostState.showSnackbar(
                    when (notice) {
                        CourseSelectionNotice.SearchFailed -> searchFailedMessage
                    },
                )
                viewModel.consumeNotice()
            }
            CourseSelectionUiState.Loading,
            CourseSelectionUiState.NotLoggedIn,
                -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = { Text(stringResource(Res.string.course_selection)) },
                navigationIcon = {
                    BackIconButton(onClick = { backStack.removeLastOrNull() })
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            CourseSelectionUiState.Loading -> LoadingContent(contentPadding = padding)

            CourseSelectionUiState.NotLoggedIn -> NotLoggedInContent(
                onLogin = { backStack.add(LoginRoute) },
                contentPadding = padding,
            )
            is CourseSelectionUiState.Error -> ErrorContent(
                throwable = current.throwable,
                contentPadding = padding,
            )
            is CourseSelectionUiState.Success -> CourseSelectionContent(
                state = current,
                onSearch = viewModel::search,
                onSelect = {
                    backStack.add(CourseOperationRoute(CourseOperation.Select, it.id, it.name))
                },
                onDrop = {
                    backStack.add(CourseOperationRoute(CourseOperation.Drop, it.id, it.name))
                },
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun CourseSelectionContent(
    state: CourseSelectionUiState.Success,
    onSearch: (String) -> Unit,
    onSelect: (SelectableCourse) -> Unit,
    onDrop: (SelectedCourse) -> Unit,
    contentPadding: PaddingValues,
) {
    var query by remember(state.query) { mutableStateOf(state.query) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.overview.entrance.name, style = MaterialTheme.typography.titleLarge)
                    Text(state.overview.entrance.semester.toString())
                    state.overview.credits.progress.forEach {
                        Text(
                            stringResource(
                                Res.string.credit_progress,
                                it.category,
                                "${it.selected}",
                                "${it.limit}",
                            ),
                        )
                    }
                    if (state.overview.credits.note.isNotBlank()) Text(state.overview.credits.note)
                }
            }
        }
        item { Text(stringResource(Res.string.selected_courses), style = MaterialTheme.typography.titleMedium) }
        if (state.overview.selectedCourses.isEmpty()) {
            item { Text(stringResource(Res.string.no_selected_courses), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.overview.selectedCourses, key = SelectedCourse::id) { course ->
                CourseCard(
                    title = course.name,
                    subtitle = stringResource(
                        Res.string.course_summary,
                        course.teacher,
                        "${course.credits}",
                        course.timeString.orEmpty(),
                    ),
                    action = stringResource(Res.string.drop_course),
                    enabled = !state.isOperating,
                    onAction = { onDrop(course) },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(Res.string.course_search_label)) },
                    singleLine = true,
                )
                Button(onClick = { onSearch(query) }, enabled = !state.isSearching) {
                    if (state.isSearching) {
                        PomLoadingIndicator(size = InlineLoadingIndicatorSize)
                    } else {
                        Text(stringResource(Res.string.search))
                    }
                }
            }
        }
        item { Text(stringResource(Res.string.selectable_general_courses), style = MaterialTheme.typography.titleMedium) }
        if (!state.isSearching && state.searchResults.isEmpty()) {
            item { Text(stringResource(Res.string.course_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.searchResults, key = SelectableCourse::id) { course ->
                CourseCard(
                    title = course.name,
                    subtitle = stringResource(
                        Res.string.selectable_course_summary,
                        course.teacher,
                        course.remaining,
                        course.capacity,
                        course.schedule,
                    ),
                    action = if (course.isSelected) {
                        stringResource(Res.string.course_selected)
                    } else {
                        stringResource(Res.string.select_course)
                    },
                    enabled = !course.isSelected && course.isOpenForSelection && !state.isOperating,
                    onAction = { onSelect(course) },
                )
            }
        }
    }

}

@Composable
private fun CourseCard(
    title: String,
    subtitle: String,
    action: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onAction, enabled = enabled) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseOperationDialog(
    route: CourseOperationRoute,
    viewModel: CourseOperationViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var operating by remember { mutableStateOf(false) }

    // 回调（onClick / fold）不是 @Composable 上下文，文案要提前取好
    val selectSucceededMessage = stringResource(Res.string.course_select_succeeded)
    val dropSucceededMessage = stringResource(Res.string.course_drop_succeeded)
    val operationFailedMessage = stringResource(Res.string.course_operation_failed)
    // 内容放进 Material3 弹窗的槽位，容器由 AlertDialog 自己提供
    AlertDialog(
        onDismissRequest = {
            // 请求进行中不许关闭：弹窗一关，承载请求的 viewModelScope 会被一起销毁，
            // 选课/退课结果就丢了，而教务那边可能已经生效，本地与教务会长期不一致。
            if (!operating) backStack.removeLastOrNull()
        },
        properties = DialogProperties(
            dismissOnBackPress = !operating,
            dismissOnClickOutside = !operating,
        ),
        title = {
            Text(
                    if (route.operation == CourseOperation.Select) {
                        stringResource(Res.string.confirm_select_course)
                    } else {
                        stringResource(Res.string.confirm_drop_course)
                    },
                )
        },
        text = {
            Text(
                if (route.operation == CourseOperation.Select) {
                    stringResource(Res.string.confirm_select_course_message, route.courseName)
                } else {
                    stringResource(Res.string.confirm_drop_course_message, route.courseName)
                },
            )
        },
        confirmButton = {
                Button(
                    enabled = !operating,
                    onClick = {
                        operating = true
                        viewModel.operate(route) { result ->
                            operating = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        { operation ->
                                            if (operation.isSuccess) {
                                                if (route.operation == CourseOperation.Select) {
                                                    selectSucceededMessage
                                                } else {
                                                    dropSucceededMessage
                                                }
                                            } else {
                                                operation.message ?: operationFailedMessage
                                            }
                                        },
                                        { it.message ?: operationFailedMessage },
                                    ),
                                )
                                if (result.getOrNull()?.isSuccess == true) backStack.removeLastOrNull()
                            }
                        }
                    },
                ) {
                    Text(
                        if (operating) {
                            stringResource(Res.string.processing)
                        } else {
                            stringResource(Res.string.confirm)
                        },
                    )
                }
        },
        dismissButton = {
            TextButton(onClick = { backStack.removeLastOrNull() }, enabled = !operating) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun CourseSelectionContentPreview() {
    PomegranateExpressiveTheme {
        CourseSelectionContent(
            state = CourseSelectionUiState.Success(
                overview = CourseSelectionOverview(
                    entrance = CourseSystemEntrance(
                        id = "preview",
                        userId = "preview",
                        semester = Semester(2025, Semester.Term.First),
                        name = "2025-2026 学年第一学期选课",
                        startTime = LocalDateTime(2025, 8, 25, 9, 0),
                        endTime = LocalDateTime(2025, 9, 5, 17, 0),
                    ),
                    credits = CreditSummary(
                        note = "请留意课程冲突和剩余名额。",
                        progress = listOf(CreditSummary.CreditProgress("通识课", "2", "6")),
                    ),
                    selectedCourses = listOf(
                        SelectedCourse("selected", "GE101", "艺术鉴赏", 2.0, "选修", "陈老师", "1-16周 星期一 1-2节", "一教 201"),
                    ),
                ),
                query = "经济",
                searchResults = listOf(previewSelectableCourse()),
            ),
            onSearch = {},
            onSelect = {},
            onDrop = {},
            contentPadding = PaddingValues.Zero,
        )
    }
}

private fun previewSelectableCourse() = SelectableCourse(
    id = "selectable", courseId = "GE202", name = "经济学导论", credits = 2.0,
    totalHours = 32, teacher = "林老师", teacherIds = "1001", schedule = "1-16周 星期三 3-4节",
    classroom = "二教 305", campusId = "2", capacity = 60, remaining = 12, enrolled = 48,
    preselected = 48, scheduledCapacity = 60, selectionCategory = "通识选修课程",
    selectionCategoryCode = "GGXX", attributeCode = "02", department = "经济学院",
    offeringUnitCode = "10", conflictDescription = "", isOpenForSelection = true,
    isSelected = false, isDeleted = false, semesterId = "2025-2026-1", courseMasterId = "master",
    offeringCode = null, courseNatureCode = "02", examMethodCode = "2", adjustmentCategory = "",
)
