package com.chow.pomegranate.ui.screen.notice

import com.chow.pomegranate.repository.NoticeRepository
import com.chow.pomegranate.service.academic.notice.api.AcademicNotice
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.chow.pomegranate.service.academic.notice.model.NoticePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMoreAppendsNextPageAndDoesNotRequestItTwice() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("first")), totalPages = 3)),
                2 to Result.success(page(2, listOf(notice("second")), totalPages = 3)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
        assertEquals(listOf("first", "second"), viewModel.uiState.value.notices.map(BasicNotice::title))
    }

    @Test
    fun failedLoadMoreCanRetryTheSamePage() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("first")), totalPages = 3)),
                2 to Result.failure(IllegalStateException("network")),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<IllegalStateException>(viewModel.uiState.value.loadMoreError)
        assertTrue(viewModel.uiState.value.canLoadMore)

        repository.pages[2] = Result.success(page(2, listOf(notice("second")), totalPages = 3))
        viewModel.retryLoadMore()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2, 2), repository.requestedPages)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(listOf("first", "second"), viewModel.uiState.value.notices.map(BasicNotice::title))
    }

    @Test
    fun lastPageStopsFurtherPagination() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("only")), totalPages = 1)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canLoadMore)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        // 已经到了末页，不应该再发出请求
        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun searchKeepsFetchingPagesUntilSomethingMatches() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("关于选课的通知")), totalPages = 3)),
                2 to Result.success(page(2, listOf(notice("关于体测的通知")), totalPages = 3)),
                3 to Result.success(page(3, listOf(notice("关于考试的通知")), totalPages = 3)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateQuery("考试")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), repository.requestedPages)
        assertEquals(listOf("关于考试的通知"), viewModel.uiState.value.filteredNotices.map(BasicNotice::title))
    }

    @Test
    fun searchStopsAtLastPageAndReportsNothingFound() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("关于选课的通知")), totalPages = 2)),
                2 to Result.success(page(2, listOf(notice("关于体测的通知")), totalPages = 2)),
                3 to Result.success(page(3, listOf(notice("不该被请求")), totalPages = 2)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateQuery("考试")
        dispatcher.scheduler.advanceUntilIdle()

        // 翻到末页就停下，不会一直“正在加载”
        assertEquals(listOf(1, 2), repository.requestedPages)
        assertTrue(viewModel.uiState.value.filteredNotices.isEmpty())
        assertTrue(viewModel.uiState.value.isSearchExhausted)
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun searchStopsPrefetchingOnceSomethingMatches() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("关于考试的通知")), totalPages = 50)),
                2 to Result.success(page(2, listOf(notice("第二页")), totalPages = 50)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateQuery("考试")
        dispatcher.scheduler.advanceUntilIdle()

        // 第一页就有结果，没必要继续把历史全拉下来
        assertEquals(listOf(1), repository.requestedPages)
        assertEquals(listOf("关于考试的通知"), viewModel.uiState.value.filteredNotices.map(BasicNotice::title))
    }

    @Test
    fun continueSearchFetchesTheNextPageOnDemand() = runTest {
        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(page(1, listOf(notice("关于考试的通知")), totalPages = 5)),
                2 to Result.success(page(2, listOf(notice("关于补考考试的通知")), totalPages = 5)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateQuery("考试")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(1), repository.requestedPages)

        viewModel.continueSearch()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.requestedPages)
        assertEquals(2, viewModel.uiState.value.filteredNotices.size)
    }

    @Test
    fun keywordSearchSpansTitleDateAndUrl() {
        val state = NoticeUiState(
            notices = listOf(notice("教学安排", "2026-09-28"), notice("考试通知", "2026-08-20")),
            query = "2026-08-20",
            isInitialLoading = false,
        )

        assertEquals(listOf("考试通知"), state.filteredNotices.map(BasicNotice::title))
        assertEquals(2, state.notices.size)
    }

    @Test
    fun todayAndYesterdayFiltersKeepOnlyMatchingDays() {
        val today = LocalDate(2026, 9, 28)
        val notices = listOf(
            notice("今天发布", "2026-09-28"),
            notice("昨天发布", "2026-09-27"),
            notice("前天发布", "2026-09-26"),
        )

        assertEquals(
            listOf("今天发布"),
            notices.filter { it.matchesFilter(NoticeFilter.Today, today) }.map(BasicNotice::title),
        )
        assertEquals(
            listOf("昨天发布"),
            notices.filter { it.matchesFilter(NoticeFilter.Yesterday, today) }.map(BasicNotice::title),
        )
        assertEquals(3, notices.filter { it.matchesFilter(NoticeFilter.All, today) }.size)
    }

    @Test
    fun weekAndMonthFiltersUseMondayAsWeekStart() {
        // 2026-09-28 是周一，2026-09-27 是上周日
        val today = LocalDate(2026, 9, 28)
        val notices = listOf(
            notice("本周周一", "2026-09-28"),
            notice("上周日", "2026-09-27"),
            notice("本月月初", "2026-09-01"),
            notice("上个月", "2026-08-31"),
        )

        assertEquals(
            listOf("本周周一"),
            notices.filter { it.matchesFilter(NoticeFilter.ThisWeek, today) }
                .map(BasicNotice::title),
        )
        assertEquals(
            listOf("本周周一", "上周日", "本月月初"),
            notices.filter { it.matchesFilter(NoticeFilter.ThisMonth, today) }
                .map(BasicNotice::title),
        )
    }

    @Test
    fun todayFilterStopsAsSoonAsPageIsOlderThanToday() = runTest {
        // 列表按时间倒序：最新一条已经不是今天，说明今天没有通知，
        // 不该继续往后翻（老实现会一路翻到最后一页，界面一直停在"正在搜索"）
        val today = currentLocalDate()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val lastWeek = today.minus(7, DateTimeUnit.DAY)

        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(
                    page(
                        1,
                        listOf(
                            notice("前天通知", lastWeek.toString()),
                            notice("昨天通知", yesterday.toString()),
                        ),
                        totalPages = 50,
                    ),
                ),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NoticeFilter.Today)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedPages)
        assertTrue(viewModel.uiState.value.filteredNotices.isEmpty())
    }

    @Test
    fun thisMonthFilterStopsOncePageIsOlderThanMonthStart() = runTest {
        // 本月筛选同理：一旦翻到上个月的日期就该停，不必翻完全部历史
        val today = currentLocalDate()
        val previousMonth = today.minus(40, DateTimeUnit.DAY)

        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(
                    page(1, listOf(notice("上个月的通知", previousMonth.toString())), totalPages = 50),
                ),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NoticeFilter.ThisMonth)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedPages)
    }

    @Test
    fun pageContainingTodayDoesNotStopMonthFilterEarly() = runTest {
        // 反向保护：只要这一页最新的一条仍在范围内，就不能提前停。
        // 否则"本月"会被一条新通知骗停，丢掉后面同月但更早的通知。
        val today = currentLocalDate()
        val previousMonth = today.minus(40, DateTimeUnit.DAY)

        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(
                    page(
                        1,
                        listOf(
                            notice("今天的通知", today.toString()),
                            notice("上个月的通知", previousMonth.toString()),
                        ),
                        totalPages = 5,
                    ),
                ),
                2 to Result.success(page(2, listOf(notice("本月早些时候", today.toString())), totalPages = 5)),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NoticeFilter.ThisMonth)
        dispatcher.scheduler.advanceUntilIdle()

        // 第 1 页有命中（今天属于本月），自动续页应停下，而不是继续翻
        assertEquals(listOf(1), repository.requestedPages)
        assertEquals(listOf("今天的通知"), viewModel.uiState.value.filteredNotices.map(BasicNotice::title))
    }

    @Test
    fun matchingPageKeepsRemainingHistoryOnDemand() = runTest {
        // 第 1 页就落在范围内（最新一条就是今天），不该为了"找全今天"把历史全拉下来；
        // 剩下的页留给滚动或"继续查找更多匹配结果"
        val today = currentLocalDate()
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(
                    page(
                        1,
                        listOf(
                            notice("今天通知", today.toString()),
                            notice("昨天通知", yesterday.toString()),
                        ),
                        totalPages = 3,
                    ),
                ),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NoticeFilter.Today)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedPages)
        assertEquals(listOf("今天通知"), viewModel.uiState.value.filteredNotices.map(BasicNotice::title))
    }

    @Test
    fun pageEntirelyBeforeTheRangeStopsWithoutFetchingMore() = runTest {
        // 整页最新的一条都早于范围起点，后面的页只会更旧，必须停下（这正是"选今天一直加载"的修复）
        val today = currentLocalDate()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val dayBeforeYesterday = today.minus(2, DateTimeUnit.DAY)

        val repository = FakeNoticeRepository(
            pages = mapOf(
                1 to Result.success(
                    page(
                        1,
                        listOf(
                            notice("昨天通知", yesterday.toString()),
                            notice("前天通知", dayBeforeYesterday.toString()),
                        ),
                        totalPages = 50,
                    ),
                ),
            ),
        )
        val viewModel = NoticeViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilter(NoticeFilter.Today)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedPages)
        assertTrue(viewModel.uiState.value.filteredNotices.isEmpty())
    }

    @Test
    fun localSearchAndFilterDoNotChangeLoadedNotices() {
        val state = NoticeUiState(
            notices = listOf(notice("教学安排", "2026-09-28"), notice("考试通知", "2026-08-20")),
            query = "2026-09",
            isInitialLoading = false,
        )

        assertEquals(listOf("教学安排"), state.filteredNotices.map(BasicNotice::title))
        assertEquals(2, state.notices.size)
    }

    private class FakeNoticeRepository(
        pages: Map<Int, Result<NoticePage>>,
    ) : NoticeRepository(NoOpAcademicNotice) {
        val pages = pages.toMutableMap()
        val requestedPages = mutableListOf<Int>()

        override suspend fun getNotices(pageIndex: Int): NoticePage {
            requestedPages += pageIndex
            return requireNotNull(pages[pageIndex]) { "未准备第 $pageIndex 页" }.getOrThrow()
        }
    }

    private companion object {
        fun notice(title: String, date: String = "2026-09-28") =
            BasicNotice(title, date, "https://example.com/$title")

        fun page(
            index: Int,
            notices: List<BasicNotice>,
            totalPages: Int,
        ) = NoticePage(
            pageIndex = index,
            notices = notices,
            totalPages = totalPages,
            hasMorePages = index < totalPages,
        )

        val NoOpAcademicNotice = object : AcademicNotice {
            override suspend fun getNotices(pageIndex: Int): NoticePage =
                NoticePage(pageIndex = pageIndex, notices = emptyList(), hasMorePages = false)

            override suspend fun getNotice(notice: BasicNotice): Notice =
                error("Not used by NoticeViewModel tests")
        }
    }
}
