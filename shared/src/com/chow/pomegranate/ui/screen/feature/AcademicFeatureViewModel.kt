package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.AcademicFeature
import com.chow.pomegranate.repository.AcademicFeatureItem
import com.chow.pomegranate.repository.AcademicFeatureRepository
import com.chow.pomegranate.repository.FeatureDataModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching

/** 学业功能 UI 状态。 */
sealed interface AcademicFeatureUiState {
    /** 等待加载。 */
    data object Idle : AcademicFeatureUiState

    /** 加载中。 */
    data object Loading : AcademicFeatureUiState

    /** 加载成功。 */
    data class Success(
        val feature: AcademicFeature,
        val items: List<AcademicFeatureItem>,
        /** 本地过滤关键词，仅 [AcademicFeature.supportsLocalSearch] 的功能会用到。 */
        val keyword: String = "",
        /** 服务端查询关键词，仅 [FeatureDataModel.Paged] 的功能会用到。 */
        val query: String = "",
        /** 是否正在请求服务端。 */
        val isLoadingPage: Boolean = false,
        /** 服务端是否还有下一页。 */
        val hasMorePages: Boolean = false,
    ) : AcademicFeatureUiState

    /** 尚未登录，不能请求学业接口。 */
    data object NotLoggedIn : AcademicFeatureUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : AcademicFeatureUiState
}

/** 学业功能 ViewModel。 */
@KoinViewModel
class AcademicFeatureViewModel(
    private val repository: AcademicFeatureRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** UI 状态。 */
    val uiState: StateFlow<AcademicFeatureUiState>
        field = MutableStateFlow<AcademicFeatureUiState>(AcademicFeatureUiState.Idle)

    /** 是否有请求在途，避免搜索、翻页与刷新互相覆盖。 */
    private var isLoading = false

    /** 下一页页码，仅分页功能有意义。 */
    private var nextPageIndex = FIRST_PAGE_INDEX

    /**
     * 加载 [feature]。
     */
    fun load(feature: AcademicFeature, force: Boolean = false) {
        if (isLoading) return
        if (!force && (uiState.value as? AcademicFeatureUiState.Success)?.feature == feature) return

        viewModelScope.launch {
            isLoading = true
            uiState.value = AcademicFeatureUiState.Loading

            try {
                // 未登录时不请求受保护接口
                if (!sessionManager.isLoggedInWithWait()) {
                    uiState.value = AcademicFeatureUiState.NotLoggedIn
                    return@launch
                }

                nextPageIndex = FIRST_PAGE_INDEX
                uiState.value = fetch(feature = feature, pageIndex = FIRST_PAGE_INDEX, append = false)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 按关键词查询服务端。
     *
     * 只对分页功能生效；其他功能仍走本地过滤，由 [submitKeyword] 处理。
     * 关键词为空表示回到不过滤的第一页。
     */
    fun search(query: String) {
        val state = uiState.value as? AcademicFeatureUiState.Success ?: return
        if (state.feature.dataModel != FeatureDataModel.Paged) return

        // 关键词没变、已有结果且不在请求中，不重复打服务端
        if (state.query == query && !state.isLoadingPage && state.items.isNotEmpty()) return

        viewModelScope.launch {
            isLoading = true
            uiState.value = state.copy(query = query, isLoadingPage = true)

            try {
                nextPageIndex = FIRST_PAGE_INDEX + 1
                uiState.value = fetch(
                    feature = state.feature,
                    pageIndex = FIRST_PAGE_INDEX,
                    append = false,
                    query = query,
                )
            } finally {
                isLoading = false
            }
        }
    }

    /** 加载下一页。 */
    fun loadMore() {
        val state = uiState.value as? AcademicFeatureUiState.Success ?: return
        if (state.feature.dataModel != FeatureDataModel.Paged) return
        if (!state.hasMorePages || isLoading) return

        viewModelScope.launch {
            isLoading = true
            uiState.value = state.copy(isLoadingPage = true)

            try {
                val pageIndex = nextPageIndex
                val next = fetch(
                    feature = state.feature,
                    pageIndex = pageIndex,
                    append = true,
                    query = state.query,
                )

                if (next is AcademicFeatureUiState.Success) {
                    nextPageIndex = pageIndex + 1
                }

                uiState.value = next
            } finally {
                isLoading = false
            }
        }
    }

    /** Applies a local keyword query to the already loaded API response. */
    fun submitKeyword(keyword: String) {
        val current = uiState.value as? AcademicFeatureUiState.Success ?: return

        uiState.value = current.copy(keyword = keyword)
    }

    /**
     * 拉取一页数据。
     *
     * 失败转成 [AcademicFeatureUiState.Error]，不把异常留给界面。
     */
    private suspend fun fetch(
        feature: AcademicFeature,
        pageIndex: Int,
        append: Boolean,
        query: String = "",
    ): AcademicFeatureUiState {
        val previous = uiState.value as? AcademicFeatureUiState.Success

        return runSuspendCatching {
            repository.getItems(feature = feature, pageIndex = pageIndex, query = query)
        }.fold(
            onSuccess = { page ->
                AcademicFeatureUiState.Success(
                    feature = feature,
                    items = if (append && previous != null) {
                        (previous.items + page.items).distinctBy(AcademicFeatureItem::id)
                    } else {
                        page.items
                    },
                    query = query,
                    hasMorePages = page.hasMorePages,
                )
            },
            onFailure = AcademicFeatureUiState::Error,
        )
    }

    private companion object {
        const val FIRST_PAGE_INDEX = 1
    }
}
