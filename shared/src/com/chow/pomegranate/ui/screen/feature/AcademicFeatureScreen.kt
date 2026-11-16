package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.chow.pomegranate.repository.AcademicFeature
import com.chow.pomegranate.repository.AcademicFeatureItem
import com.chow.pomegranate.repository.FeatureDataModel
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_search_hint
import com.chow.pomegranate.teacher_search_hint
import com.chow.pomegranate.keyword
import com.chow.pomegranate.teachers
import com.chow.pomegranate.extracurricular
import com.chow.pomegranate.course_timetable
import com.chow.pomegranate.academic_progress
import com.chow.pomegranate.academic_plan
import androidx.compose.material3.FilledTonalButton
import com.chow.pomegranate.ui.component.PomLoadingIndicator
import com.chow.pomegranate.ui.component.InlineLoadingIndicatorSize
import com.chow.pomegranate.load_more_results
import kotlinx.coroutines.delay
import com.chow.pomegranate.academic_data
import com.chow.pomegranate.academic_record_count
import com.chow.pomegranate.load_failed
import com.chow.pomegranate.no_academic_data
import com.chow.pomegranate.refresh
import com.chow.pomegranate.view_details
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.popDestination
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.ui.component.NotLoggedInContent
import com.chow.pomegranate.ui.screen.login.LoginRoute
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

/** 选课系统页路由。 */
@Serializable
data object CourseSelectionRoute : NavKey

/** 第二课堂页路由。 */
@Serializable
data object ExtracurricularRoute : NavKey

/** 教师信息页路由。 */
@Serializable
data object TeachersRoute : NavKey

/** 全校课表页路由。 */
@Serializable
data object CourseTimetableRoute : NavKey

/** 执行计划页路由。 */
@Serializable
data object AcademicPlanRoute : NavKey

/** 学业进度页路由。 */
@Serializable
data object AcademicProgressRoute : NavKey

/** 功能详情弹窗路由。 */
@Serializable
data class FeatureDetailRoute(
    val title: String,
    val details: Map<String, String>,
) : NavKey

/**
 * 注册学业功能导航入口。
 */
fun EntryProviderScope<NavKey>.academicFeatureEntries() {
    entry<CourseSelectionRoute> {
        CourseSelectionScreen()
    }
    entry<CourseOperationRoute>(metadata = DialogSceneStrategy.dialog()) { route ->
        CourseOperationDialog(route)
    }
    entry<ExtracurricularRoute> {
        AcademicFeatureScreen(
            stringResource(Res.string.extracurricular),
            AcademicFeature.Extracurricular,
        )
    }
    entry<TeachersRoute> {
        AcademicFeatureScreen(
            stringResource(Res.string.teachers),
            AcademicFeature.Teachers,
        )
    }
    entry<CourseTimetableRoute> {
        AcademicFeatureScreen(
            stringResource(Res.string.course_timetable),
            AcademicFeature.CourseTimetable,
        )
    }
    entry<AcademicPlanRoute> {
        AcademicFeatureScreen(
            stringResource(Res.string.academic_plan),
            AcademicFeature.AcademicPlan,
        )
    }
    entry<AcademicProgressRoute> {
        AcademicFeatureScreen(
            stringResource(Res.string.academic_progress),
            AcademicFeature.AcademicProgress,
        )
    }
    entry<FeatureDetailRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { route ->
        val backStack = LocalBackStack.current
        FeatureDetailDialog(
            route = route,
            onDismiss = { backStack.popDestination() },
        )
    }
}

/** 学业功能页。 */
@Composable
private fun AcademicFeatureScreen(
    title: String,
    feature: AcademicFeature,
    modifier: Modifier = Modifier,
    viewModel: AcademicFeatureViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val uiState by viewModel.uiState.collectAsState()
    val loadFailedMessage = stringResource(Res.string.load_failed)

    LaunchedEffect(feature) {
        viewModel.load(feature)
    }

    LaunchedEffect(uiState) {
        val error = uiState as? AcademicFeatureUiState.Error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = error.throwable.message ?: loadFailedMessage,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    BackIconButton(onClick = { backStack.removeLastOrNull() })
                },
                actions = {
                    FilledTonalIconButton(onClick = { viewModel.load(feature, force = true) }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding + PaddingValues(16.dp)

        when (val state = uiState) {
            AcademicFeatureUiState.Idle,
            AcademicFeatureUiState.Loading,
                -> LoadingContent(contentPadding = contentPadding)
            AcademicFeatureUiState.NotLoggedIn -> NotLoggedInContent(
                onLogin = { backStack.add(LoginRoute) },
                contentPadding = contentPadding,
            )


            is AcademicFeatureUiState.Error -> ErrorContent(
                throwable = state.throwable,
                contentPadding = contentPadding,
            )

            is AcademicFeatureUiState.Success -> FeatureItems(
                items = state.items,
                keyword = state.keyword,
                onSubmitKeyword = viewModel::submitKeyword,
                searchable = state.feature.isSearchable,
                // 服务端搜索：输入即查，带防抖；本地过滤的功能只更新草稿
                serverSearch = state.feature.dataModel == FeatureDataModel.Paged,
                query = state.query,
                isSearching = state.isLoadingPage,
                hasMorePages = state.hasMorePages,
                onQueryChange = viewModel::search,
                onLoadMore = viewModel::loadMore,
                onItemClick = { item ->
                    backStack.add(
                        FeatureDetailRoute(
                            title = item.title,
                            details = item.details.toMap(),
                        ),
                    )
                },
                contentPadding = contentPadding,
            )
        }
    }
}

/** 学业功能列表。 */
@Composable
private fun FeatureItems(
    items: List<AcademicFeatureItem>,
    keyword: String,
    onSubmitKeyword: (String) -> Unit,
    searchable: Boolean,
    serverSearch: Boolean,
    query: String,
    isSearching: Boolean,
    hasMorePages: Boolean,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (AcademicFeatureItem) -> Unit,
    contentPadding: PaddingValues,
) {
    // 本地过滤的功能：草稿由用户按搜索键提交
    var draftQuery by androidx.compose.runtime.saveable.rememberSaveable(keyword) {
        androidx.compose.runtime.mutableStateOf(keyword)
    }
    // 服务端搜索的功能：输入即查，防抖 400ms，避免每敲一个字打一次服务端
    var typedQuery by androidx.compose.runtime.saveable.rememberSaveable(query) {
        androidx.compose.runtime.mutableStateOf(query)
    }

    if (serverSearch) {
        LaunchedEffect(typedQuery) {
            if (typedQuery == query) return@LaunchedEffect
            delay(SEARCH_DEBOUNCE_MILLIS)
            onQueryChange(typedQuery)
        }
    }

    // 服务端已经把关键词过滤过了，本地只对"一次返回全量"的功能再过滤
    val visibleItems = if (serverSearch) items else items.filterByKeyword(keyword)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FeatureHeader(itemCount = items.size)
        }

        if (searchable) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = if (serverSearch) typedQuery else draftQuery,
                    onValueChange = { value ->
                        if (serverSearch) {
                            typedQuery = value
                        } else {
                            draftQuery = value
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.keyword)) },
                    placeholder = {
                        Text(
                            if (serverSearch) {
                                stringResource(Res.string.teacher_search_hint)
                            } else {
                                stringResource(Res.string.academic_search_hint)
                            },
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (serverSearch && isSearching) {
                            PomLoadingIndicator(size = InlineLoadingIndicatorSize)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (serverSearch) {
                                onQueryChange(typedQuery)
                            } else {
                                onSubmitKeyword(draftQuery)
                            }
                        },
                    ),
                )
            }
        }

        if (visibleItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyFeatureContent(hasKeyword = keyword.isNotBlank() || query.isNotBlank())
            }
        }

        items(
            items = visibleItems,
            key = AcademicFeatureItem::id,
        ) { item ->
            Card(
                onClick = { onItemClick(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        if (item.supportingText.isNotBlank()) {
                            Text(item.supportingText)
                        }
                    },
                    trailingContent = {
                        FilledTonalIconButton(onClick = { onItemClick(item) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = stringResource(Res.string.view_details),
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }

        // 服务端查询还有下一页时给一个入口，不自动预取（列表可能很长）
        if (serverSearch && hasMorePages) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FilledTonalButton(
                    onClick = onLoadMore,
                    enabled = !isSearching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.load_more_results))
                }
            }
        }
    }
}

@Preview
@Composable
private fun FeatureItemsPreview() {
    PomegranateExpressiveTheme {
        FeatureItems(
            items = listOf(
                AcademicFeatureItem(
                    id = "CS101",
                    title = "程序设计",
                    supportingText = "陈老师 · 实验楼 201",
                    details = listOf(
                        "课程编号" to "CS101",
                        "学分" to "3.0",
                    ),
                ),
                AcademicFeatureItem(
                    id = "CS102",
                    title = "数据结构",
                    supportingText = "林老师 · 第一教学楼 305",
                    details = listOf(
                        "课程编号" to "CS102",
                        "学分" to "4.0",
                    ),
                ),
            ),
            keyword = "",
            onSubmitKeyword = {},
            searchable = true,
            serverSearch = false,
            query = "",
            isSearching = false,
            hasMorePages = false,
            onQueryChange = {},
            onLoadMore = {},
            onItemClick = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}

/** 功能页概览。 */
@Composable
private fun FeatureHeader(
    itemCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.School,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.academic_data),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(Res.string.academic_record_count, itemCount),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 空数据内容。 */
@Composable
private fun EmptyFeatureContent(hasKeyword: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            if (hasKeyword) "没有匹配的记录" else stringResource(Res.string.no_academic_data),
            modifier = Modifier.padding(32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** 功能详情弹窗。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureDetailDialog(
    route: FeatureDetailRoute,
    onDismiss: () -> Unit,
) {
    // 详情项，过滤掉空值
    val details = route.details.filterValues { it.isNotBlank() }.entries.toList()

    // 内容放进 Material3 弹窗的槽位，容器由 AlertDialog 自己提供
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(route.title)
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(details) { (label, value) ->
                    Column {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(value)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

/** 服务端搜索的防抖时长，避免每敲一个字打一次服务端。 */
private const val SEARCH_DEBOUNCE_MILLIS = 400L
