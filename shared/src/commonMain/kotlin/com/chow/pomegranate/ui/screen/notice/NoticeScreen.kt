package com.chow.pomegranate.ui.screen.notice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.twotone.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.*
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.noticeEntry() {
    entry<NoticeRoute>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { EmptyContent() })) {
        NoticeScreen()
    }
}

@Serializable
data object NoticeRoute : NavKey

@Composable
private fun NoticeScreen(modifier: Modifier = Modifier, viewModel: NoticeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    NoticeContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        onContinueSearch = viewModel::continueSearch,
        onQueryChange = viewModel::updateQuery,
        onFilterChange = viewModel::updateFilter,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun NoticeContentPreview() {
    PomegranateExpressiveTheme {
        NoticeContent(
            uiState = NoticeUiState(
                notices = listOf(BasicNotice("关于教学安排的通知", "2026-09-28", "https://jwc.gdufe.edu.cn/notice")),
                canLoadMore = true,
                currentPageIndex = 1,
            ),
            onRefresh = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onContinueSearch = {},
            onQueryChange = {},
            onFilterChange = {},
        )
    }
}

@Composable
private fun NoticeContent(
    uiState: NoticeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onContinueSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (NoticeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val loadFailedMessage = stringResource(Res.string.load_failed)
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()
    val sky = rememberSky()
    var showScrollToTop by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.initialError) {
        uiState.initialError?.let {
            snackbarHostState.showSnackbar(it.message ?: loadFailedMessage)
        }
    }
    LaunchedEffect(uiState.loadMoreError) {
        uiState.loadMoreError?.let {
            snackbarHostState.showSnackbar(it.message ?: loadFailedMessage)
        }
    }
    LaunchedEffect(gridState, uiState.notices.size, uiState.canLoadMore, uiState.isLoadingMore) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
        }.collect { (lastVisible, total) ->
            showScrollToTop = gridState.firstVisibleItemIndex > 0
            if (lastVisible >= total - LOAD_MORE_THRESHOLD) onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            NoticeTopBar(
                query = uiState.query,
                filter = uiState.filter,
                onQueryChange = onQueryChange,
                onFilterChange = onFilterChange,
                onBack = { backStack.removeLastOrNull() },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
            ) {
                SmallFloatingActionButton(onClick = { scope.launch { gridState.animateScrollToItem(0) } }) {
                    Icon(Icons.Rounded.ArrowUpward, stringResource(Res.string.academic_notice))
                }
            }
        },
    ) { innerPadding ->
        // 不能把 innerPadding 加在外层容器上：那样列表的绘制区域会从顶栏下方开始，
        // 毛玻璃背后没有任何内容可采样，模糊效果就会画错位。
        // 这里沿用其他页面的做法——滚动区域铺满（含顶栏后方），由列表自己的
        // contentPadding 让内容避开顶栏。
        Box(Modifier.fillMaxSize()) {
            when {
                uiState.isInitialLoading && uiState.notices.isEmpty() ->
                    LoadingContent(contentPadding = innerPadding)

                uiState.initialError != null && uiState.notices.isEmpty() -> {
                    ErrorContent(throwable = uiState.initialError, contentPadding = innerPadding)

                    Button(
                        onClick = onRefresh,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(innerPadding)
                            .padding(24.dp),
                    ) {
                        Text(stringResource(Res.string.retry))
                    }
                }

                // 搜索还在把后续页取回来时，不能直接给“没有数据”的结论
                uiState.hasActiveSearch && uiState.filteredNotices.isEmpty() && uiState.isSearchLoading ->
                    SearchingContent(
                        pageIndex = uiState.currentPageIndex,
                        contentPadding = innerPadding,
                    )

                // 已翻完全部页仍没有命中
                uiState.isSearchExhausted && uiState.filteredNotices.isEmpty() ->
                    EmptyContent(
                        contentPadding = innerPadding,
                        message = if (uiState.query.isBlank()) {
                            // 只按时间范围筛选时，没有关键词可展示
                            stringResource(
                                Res.string.notice_not_found_in_range,
                                uiState.lastPageIndex ?: uiState.currentPageIndex,
                            )
                        } else {
                            stringResource(
                                Res.string.notice_not_found,
                                uiState.lastPageIndex ?: uiState.currentPageIndex,
                                uiState.query.trim(),
                            )
                        },
                    )

                uiState.filteredNotices.isEmpty() -> EmptyContent(contentPadding = innerPadding)

                else -> NoticeList(
                    notices = uiState.filteredNotices,
                    uiState = uiState,
                    gridState = gridState,
                    skyModifier = Modifier.sky(sky = sky),
                    contentPadding = innerPadding,
                    onRetryLoadMore = onRetryLoadMore,
                    onContinueSearch = onContinueSearch,
                    onOpenInBrowser = uriHandler::openUri,
                ) { notice -> backStack.add(NoticeDetailRoute(notice.title, notice.date, notice.urlString)) }
            }
        }
    }
}

/**
 * 顶部导航。
 *
 * 搜索栏与筛选标签都并进同一层毛玻璃容器，和标签行一样的观感，不再是内容区里
 * 单独一块输入框。
 */
@Composable
private fun NoticeTopBar(
    query: String,
    filter: NoticeFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (NoticeFilter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PomSearchTopBar(
        title = { Text(stringResource(Res.string.academic_notice)) },
        modifier = modifier,
        navigationIcon = { BackIconButton(onClick = onBack) },
        searchBar = {
            PomSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(Res.string.notice_search),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        },
        footer = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(NoticeFilter.entries) { option ->
                    val label = when (option) {
                        NoticeFilter.All -> stringResource(Res.string.notice_filter_all)
                        NoticeFilter.Today -> stringResource(Res.string.notice_filter_today)
                        NoticeFilter.Yesterday -> stringResource(Res.string.notice_filter_yesterday)
                        NoticeFilter.ThisWeek -> stringResource(Res.string.notice_filter_this_week)
                        NoticeFilter.ThisMonth -> stringResource(Res.string.notice_filter_this_month)
                    }
                    FilterChip(filter == option, { onFilterChange(option) }, label = { Text(label) })
                }
            }
        },
    )
}

/** 正在搜索后续页时的占位内容。 */
@Composable
private fun SearchingContent(
    pageIndex: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PomLoadingIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(Res.string.searching_notices, pageIndex),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeList(
    notices: List<BasicNotice>,
    uiState: NoticeUiState,
    gridState: LazyStaggeredGridState,
    skyModifier: Modifier,
    contentPadding: PaddingValues,
    onRetryLoadMore: () -> Unit,
    onContinueSearch: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onOpenNotice: (BasicNotice) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp), modifier = skyModifier.fillMaxSize(), state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalItemSpacing = 16.dp, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(notices, key = BasicNotice::urlString) { notice ->
            NoticeCard(notice, { onOpenInBrowser(notice.urlString) }, { onOpenNotice(notice) })
        }

        // 搜索有结果但可能还有更多匹配时，给一个继续查找的入口
        if (uiState.hasActiveSearch && !uiState.isLoadingMore && uiState.canLoadMore && !uiState.isSearchExhausted) {
            item(span = StaggeredGridItemSpan.FullLine) {
                TextButton(
                    onClick = onContinueSearch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.search_more))
                }
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            LoadMoreFooter(
                uiState = uiState,
                onRetryLoadMore = onRetryLoadMore,
            )
        }
    }
}

/** 列表底部状态。 */
@Composable
private fun LoadMoreFooter(
    uiState: NoticeUiState,
    onRetryLoadMore: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // 搜索续页时给出行内进度，横跨整行的 48dp 转圈在列表底部太抢眼
            uiState.isLoadingMore && uiState.isSearchLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PomLoadingIndicator(size = InlineLoadingIndicatorSize)

                Text(
                    stringResource(Res.string.searching_notices, uiState.currentPageIndex),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.isLoadingMore -> PomLoadingIndicator(size = InlineLoadingIndicatorSize)

            uiState.loadMoreError != null ->
                TextButton(onClick = onRetryLoadMore) { Text(stringResource(Res.string.retry)) }

            !uiState.canLoadMore ->
                Text(
                    stringResource(Res.string.no_more_notices),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            else -> Text(
                stringResource(Res.string.pull_up_to_load_more),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoticeCard(notice: BasicNotice, onOpenInBrowser: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(notice.title, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(notice.date)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notice.urlString, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onOpenInBrowser) {
                    Icon(Icons.TwoTone.OpenInBrowser, stringResource(Res.string.open_in_browser))
                }
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 4
