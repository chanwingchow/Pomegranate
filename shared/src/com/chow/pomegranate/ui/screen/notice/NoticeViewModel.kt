package com.chow.pomegranate.ui.screen.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.NoticeRepository
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.NoticePage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/** 教学通知页 UI 状态。 */
data class NoticeUiState(
    val notices: List<BasicNotice> = emptyList(),
    val query: String = "",
    val filter: NoticeFilter = NoticeFilter.All,
    val isInitialLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isSearchLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val currentPageIndex: Int = 0,
    val lastPageIndex: Int? = null,
    val initialError: Throwable? = null,
    val loadMoreError: Throwable? = null,
    /** 今天的日期，用于按时间范围筛选；随每次拉取刷新。 */
    val today: LocalDate = currentLocalDate(),
) {
    /** 应用本地关键词和时间范围筛选后的通知。 */
    val filteredNotices: List<BasicNotice>
        get() = notices.filter { notice -> notice.matchesKeyword(query, filter, today) }

    /** 是否存在会影响结果集的关键词或筛选条件。 */
    val hasActiveSearch: Boolean
        get() = query.isNotBlank() || filter != NoticeFilter.All

    /**
     * 关键词已经搜完全部已加载的页，但一条都没命中。
     *
     * 此时再继续翻页只是浪费请求，界面应该给“没有匹配结果”的结论。
     */
    val isSearchExhausted: Boolean
        get() = hasActiveSearch &&
            filteredNotices.isEmpty() &&
            !isInitialLoading &&
            !isLoadingMore &&
            !canLoadMore
}

/**
 * 当前本地日期。
 *
 * 单独抽出来既是给状态一个默认值，也方便测试里替换成固定日期。
 */
internal fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * 按关键词和 [filter] 指定的时间范围匹配。
 *
 * 界面筛选与 ViewModel 的续页判定共用这一份规则，避免两处行为不一致。
 */
internal fun BasicNotice.matchesKeyword(
    keyword: String,
    filter: NoticeFilter = NoticeFilter.All,
    today: LocalDate = currentLocalDate(),
): Boolean {
    if (!matchesFilter(filter, today)) return false

    val trimmed = keyword.trim()
    if (trimmed.isEmpty()) return true

    return listOf(title, date, urlString)
        .any { it.contains(trimmed, ignoreCase = true) }
}

/**
 * 判断发布日期是否落在 [filter] 的时间范围内。
 *
 * 列表里的日期是 `yyyy-MM-dd`，本身就是可比较的字典序，直接比较字符串即可，
 * 不用为每条通知解析日期。
 */
internal fun BasicNotice.matchesFilter(
    filter: NoticeFilter,
    today: LocalDate = currentLocalDate(),
): Boolean = when (filter) {
    NoticeFilter.All -> true
    NoticeFilter.Today -> date == today.toString()
    NoticeFilter.Yesterday -> date == today.minus(1, DateTimeUnit.DAY).toString()
    NoticeFilter.ThisWeek -> date in today.startOfWeekIso()..today.toString()
    NoticeFilter.ThisMonth -> date.startsWith("${today.year}-${today.monthNumber.twoDigits()}")
}

/**
 * [filter] 时间范围的起点，早于它就已经翻过头了。
 *
 * 列表按时间倒序，越往后越旧；日期一旦早于起点，后面的页只会更早，
 * 不可能再有命中——继续翻只是白白打请求（选"今天"而今天没有通知时，
 * 老实现会一路翻到最后一页，界面一直停在"正在搜索"）。
 */
private fun NoticeFilter.rangeStart(today: LocalDate): String? = when (this) {
    NoticeFilter.All -> null
    NoticeFilter.Today -> today.toString()
    NoticeFilter.Yesterday -> today.minus(1, DateTimeUnit.DAY).toString()
    NoticeFilter.ThisWeek -> today.startOfWeekIso()
    NoticeFilter.ThisMonth -> "${today.year}-${today.monthNumber.twoDigits()}-01"
}

/**
 * 这一页是否已经完全落到 [filter] 的时间范围之前。
 *
 * 列表按时间倒序，只要**这一页最新的一条**都早于范围起点，后面的页只会更旧，
 * 不可能再有命中——继续翻纯属浪费请求（选"今天"而今天没发通知时，老实现会
 * 一路翻到最后一页，界面一直停在"正在搜索"）。
 *
 * 注意不能写成"出现过一条旧的就停"：今天完全可能排在昨天、前天后面，
 * 那样会把真正的命中一起丢掉。
 */
internal fun List<BasicNotice>.isPastFilterRange(
    filter: NoticeFilter,
    today: LocalDate = currentLocalDate(),
): Boolean {
    val start = filter.rangeStart(today) ?: return false
    val newest = maxOfOrNull { it.date } ?: return false

    return newest < start
}

/** 通知的时间范围筛选。 */
enum class NoticeFilter {
    /** 不限时间。 */
    All,

    /** 今天发布。 */
    Today,

    /** 昨天发布。 */
    Yesterday,

    /** 本周发布，按周一开始。 */
    ThisWeek,

    /** 本月发布。 */
    ThisMonth,
}

/** 补齐月份为两位，便于和 `yyyy-MM-dd` 前缀比较。 */
private fun Int.twoDigits(): String = toString().padStart(2, '0')

/** 本周周一，ISO 周从周一开始。 */
private fun LocalDate.startOfWeekIso(): String {
    val daysFromMonday = dayOfWeek.isoDayNumber - 1

    return minus(daysFromMonday, DateTimeUnit.DAY).toString()
}

/** 教学通知页 ViewModel。 */
@KoinViewModel
class NoticeViewModel(
    private val repository: NoticeRepository,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<NoticeUiState>
        field = MutableStateFlow(NoticeUiState())

    /** 下一批要加载的页码。 */
    private var nextPageIndex = FIRST_PAGE_INDEX

    /** 是否有请求在途，防止重复触发。 */
    private var isLoading = false

    init {
        refresh()
    }

    /** 刷新通知列表，回到第一页。 */
    fun refresh() {
        if (isLoading) return
        searchSettled = false
        loadPage(
            pageIndex = FIRST_PAGE_INDEX,
            isRefresh = true,
            searchQuery = null,
        )
    }

    /**
     * 加载下一页；已有请求、无更多数据、首屏加载期间或上次翻页失败时忽略重复触发。
     */
    fun loadMore() {
        val state = uiState.value
        if (state.isInitialLoading || !state.canLoadMore) return

        // 上一次翻页失败了就不再自动重试：滚动监听会在每次状态变化后再次触发，
        // 否则断网时会无退避地反复请求，界面也一直停在失败态。重试交给 retryLoadMore()。
        if (state.loadMoreError != null) return

        // 用户主动翻页时重新打开续搜，命中后仍可继续往下找
        searchSettled = false

        loadPage(
            pageIndex = nextPageIndex,
            isRefresh = false,
            searchQuery = activeSearchKey(state),
        )
    }

    /**
     * 重试上一次失败的分页请求。
     *
     * 失败时 [nextPageIndex] 没有被推进，直接沿用即可。
     */
    fun retryLoadMore() {
        if (uiState.value.loadMoreError == null) {
            // 没有可重试的错误，退化为普通翻页
            loadMore()
            return
        }

        searchSettled = false

        loadPage(
            pageIndex = nextPageIndex,
            isRefresh = false,
            searchQuery = activeSearchKey(uiState.value),
        )
    }

    /** 更新本地关键词，不会取消或阻塞分页请求。 */
    fun updateQuery(query: String) {
        val previousQuery = uiState.value.query
        if (previousQuery == query) return

        // 关键词一改，之前的“已搜完”结论就作废了
        searchSettled = false
        uiState.value = uiState.value.copy(query = query, loadMoreError = null)

        // 已加载的页里没有命中，就继续往后翻，直到命中或翻完
        search()
    }

    /** 更新本地筛选范围，不会取消或阻塞分页请求。 */
    fun updateFilter(filter: NoticeFilter) {
        if (uiState.value.filter == filter) return

        searchSettled = false
        uiState.value = uiState.value.copy(filter = filter, loadMoreError = null)

        // 换成标题或日期范围后，已加载的页里可能没有命中，继续往后找
        search()
    }

    /**
     * 继续查找更多匹配结果。
     *
     * 自动续搜在命中后就会停下，避免把整份历史都拉下来；结果不满一屏时界面
     * 提供一个入口，让用户按需要继续往后找。
     */
    fun continueSearch() {
        if (!uiState.value.hasActiveSearch) return

        searchSettled = false
        search(force = true)
    }

    /**
     * 在尚未加载的页里继续查找关键词。
     *
     * 通告列表本身是分页的，本地筛选只能覆盖已加载的内容；用户输入关键词或切换
     * 筛选范围后需要把后续页也取回来，否则永远搜不到后面的通知。
     *
     * @param force 用户明确要求继续查找时置 `true`，此时即使已有命中也会再翻一页
     */
    private fun search(force: Boolean = false) {
        val state = uiState.value

        // 没有关键词也没有筛选条件时就是普通浏览，不需要预取
        if (!state.hasActiveSearch) return

        // 已经命中、或已经把所有页都翻完了，就不再自动续页
        if (searchSettled) return

        // 已加载的页里就有命中，没必要为了搜索再取一页
        if (!force && hasMatch(state)) {
            searchSettled = true
            return
        }

        // 已加载的内容里最新一条都已经早于筛选范围，后面的页只会更旧，不必再翻。
        // 用户切换筛选时会直接走到这里，所以这道判断不能只放在 applySearchProgress 里。
        if (state.notices.isPastFilterRange(state.filter, state.today)) {
            searchSettled = true
            return
        }

        // 首屏还没落地时无法判断是否命中，等它回来再决定要不要继续翻
        if (state.isInitialLoading) return

        // 上一批还没回来，等它回调时再继续
        if (isLoading) return

        if (!state.canLoadMore) {
            searchSettled = true
            return
        }

        loadPage(
            pageIndex = nextPageIndex,
            isRefresh = false,
            searchQuery = activeSearchKey(state),
        )
    }

    /**
     * 拉取一页数据。
     *
     * @param pageIndex 目标页码
     * @param isRefresh 是否作为首屏刷新（替换列表而不是追加）
     * @param searchQuery 本次请求服务的关键词，`null` 表示普通翻页
     */
    private fun loadPage(
        pageIndex: Int,
        isRefresh: Boolean,
        searchQuery: String?,
    ) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            uiState.value = if (isRefresh) {
                uiState.value.copy(
                    notices = emptyList(),
                    isInitialLoading = true,
                    isLoadingMore = false,
                    isSearchLoading = false,
                    canLoadMore = true,
                    initialError = null,
                    loadMoreError = null,
                    currentPageIndex = 0,
                    lastPageIndex = null,
                    // 时间范围筛选依赖"今天"，每次拉取都刷新，跨零点后不会还算成昨天
                    today = currentLocalDate(),
                )
            } else {
                uiState.value.copy(
                    isLoadingMore = true,
                    isSearchLoading = searchQuery != null,
                    loadMoreError = null,
                    today = currentLocalDate(),
                )
            }

            runCatching { repository.getNotices(pageIndex) }
                .onSuccess { page ->
                    isLoading = false
                    applyPage(page = page, isRefresh = isRefresh, searchQuery = searchQuery)
                }
                .onFailure { throwable ->
                    isLoading = false
                    uiState.value = if (isRefresh) {
                        uiState.value.copy(
                            isInitialLoading = false,
                            initialError = throwable,
                        )
                    } else {
                        uiState.value.copy(
                            isLoadingMore = false,
                            isSearchLoading = false,
                            loadMoreError = throwable,
                        )
                    }
                }
        }
    }

    /** 把新一页合并进状态，并决定是否继续搜索。 */
    private fun applyPage(
        page: NoticePage,
        isRefresh: Boolean,
        searchQuery: String?,
    ) {
        val previous = uiState.value
        val merged = if (isRefresh) {
            page.notices.distinctBy(BasicNotice::urlString)
        } else {
            (previous.notices + page.notices).distinctBy(BasicNotice::urlString)
        }

        // 末页判断交给服务端的总页数；没有总页数时退化为“本页非空”
        val reachedLastPage = !page.hasMorePages
        val lastPageIndex = page.totalPages ?: page.pageIndex.takeIf { reachedLastPage }
        val canLoadMore = if (reachedLastPage) false else true

        if (reachedLastPage) {
            searchSettled = true
        }

        nextPageIndex = if (reachedLastPage) page.pageIndex else page.pageIndex + 1

        uiState.value = previous.copy(
            notices = merged,
            isInitialLoading = false,
            isLoadingMore = false,
            isSearchLoading = false,
            canLoadMore = canLoadMore,
            currentPageIndex = page.pageIndex,
            lastPageIndex = lastPageIndex,
            initialError = null,
            // 时间范围筛选依赖“今天”，跨零点后重新取一次
            today = currentLocalDate(),
        )

        // 刷新时也要照顾当前正在进行的搜索：首屏回来后继续把后续页补齐
        val effectiveSearchKey = searchQuery ?: activeSearchKey(uiState.value).takeIf { isRefresh }
        applySearchProgress(searchQuery = effectiveSearchKey, notices = merged)
    }

    /**
     * 搜索续页的收尾逻辑。
     *
     * 命中就停下，否则在没有更多页时给出“搜索完了”的结论；关键词或筛选范围在请求
     * 期间被改掉时直接放弃，避免用旧条件的结果去结束新条件的搜索。
     */
    private fun applySearchProgress(
        searchQuery: String?,
        notices: List<BasicNotice>,
    ) {
        if (searchQuery == null) return

        val state = uiState.value
        if (activeSearchKey(state) != searchQuery) return

        // 已经翻到比筛选范围更早的日期：后面的页只会更旧，不可能再有命中，直接收工。
        // 这一步是"选今天却一直加载"的关键——今天没有通知时不该把历史全翻一遍。
        if (notices.isPastFilterRange(state.filter, state.today)) {
            searchSettled = true
            return
        }

        // 空串是“只有时间范围筛选”的占位键，此时也要确认这一页真的落在范围内
        val matched = notices.any { it.matchesKeyword(searchQuery, state.filter, state.today) }
        if (matched) {
            // 已经有结果，剩下的页等用户滚动或点“继续查找”时再加载
            searchSettled = true
            return
        }

        if (!state.canLoadMore) {
            searchSettled = true
            return
        }

        search()
    }

    /**
     * 已加载的通知里是否已经满足当前搜索条件。
     *
     * 只有时间范围筛选时，命中的判定同样交给 [matchesKeyword]，因为通知列表按时间
     * 倒序排列，翻到范围外的页就不该再翻了。
     */
    private fun hasMatch(state: NoticeUiState): Boolean {
        if (!state.hasActiveSearch) return false

        return state.notices.any {
            it.matchesKeyword(state.query, state.filter, state.today)
        }
    }

    /** 当前续页请求对应的搜索键，`null` 表示普通翻页。 */
    private fun activeSearchKey(state: NoticeUiState): String? = when {
        !state.hasActiveSearch -> null
        // 只有筛选范围时用空串占位，避免和“没有搜索”混淆
        state.query.isBlank() -> ""
        else -> state.query.trim()
    }

    private companion object {
        const val FIRST_PAGE_INDEX = 1
    }

    /** 是否已经得出搜索结论，避免无限续页。 */
    private var searchSettled = false
}
