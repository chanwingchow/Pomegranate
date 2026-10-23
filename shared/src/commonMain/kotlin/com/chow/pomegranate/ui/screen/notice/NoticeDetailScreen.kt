package com.chow.pomegranate.ui.screen.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.OpenInBrowser
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_notice
import com.chow.pomegranate.open_in_browser
import com.chow.pomegranate.open_link_failed
import com.chow.pomegranate.repository.NoticeDetail
import com.chow.pomegranate.repository.NoticeRepository
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import de.charlex.compose.htmltext.material3.HtmlText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy

/**
 * 通知详情页路由。
 *
 * 带上列表里已有的字段，进入页面直接用它们取详情。
 */
@Serializable
data class NoticeDetailRoute(
    val title: String = "",
    val date: String = "",
    val urlString: String = "",
) : NavKey

/** 通知详情页 UI 状态。 */
sealed interface NoticeDetailUiState {
    /** 加载中。 */
    data object Loading : NoticeDetailUiState

    /** 加载成功。 */
    data class Success(val detail: NoticeDetail) : NoticeDetailUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : NoticeDetailUiState
}

/** 通知详情页 ViewModel。 */
@KoinViewModel
class NoticeDetailViewModel(
    private val repository: NoticeRepository,
) : ViewModel() {
    /** 当前 UI 状态。 */
    val uiState: StateFlow<NoticeDetailUiState>
        field = MutableStateFlow<NoticeDetailUiState>(NoticeDetailUiState.Loading)

    /** 已加载的通知链接，避免重复请求。 */
    private var loadedUrl: String? = null

    /** 加载 [notice] 的详情。 */
    fun load(notice: BasicNotice) {
        if (loadedUrl == notice.urlString && uiState.value is NoticeDetailUiState.Success) return

        loadedUrl = notice.urlString

        viewModelScope.launch {
            uiState.value = NoticeDetailUiState.Loading
            uiState.value = runCatching { repository.getNotice(notice) }
                .fold(
                    onSuccess = NoticeDetailUiState::Success,
                    onFailure = NoticeDetailUiState::Error,
                )
        }
    }
}

/**
 * 注册通知详情页导航入口。
 *
 * 标记为详情面板，宽屏时与列表并排展示。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.noticeDetailEntry() {
    entry<NoticeDetailRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { route ->
        NoticeDetailScreen(
            notice = BasicNotice(
                title = route.title,
                date = route.date,
                urlString = route.urlString,
            ),
        )
    }
}

/**
 * 通知详情页。
 */
@Composable
private fun NoticeDetailScreen(
    notice: BasicNotice,
    modifier: Modifier = Modifier,
    viewModel: NoticeDetailViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val openLinkFailedMessage = stringResource(Res.string.open_link_failed)
    val openLink: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar(openLinkFailedMessage) }
        }
    }

    // UI 状态
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(notice.urlString) {
        viewModel.load(notice)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.academic_notice),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    BackIconButton(onClick = { backStack.removeLastOrNull() })
                },
                actions = {
                    // 在浏览器打开
                    FilledTonalIconButton(
                        onClick = { openLink(notice.urlString) },
                    ) {
                        Icon(
                            Icons.TwoTone.OpenInBrowser,
                            contentDescription = stringResource(Res.string.open_in_browser),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding + PaddingValues(16.dp)

        when (val state = uiState) {
            is NoticeDetailUiState.Loading -> LoadingContent(contentPadding = contentPadding)

            is NoticeDetailUiState.Error -> ErrorContent(
                throwable = state.throwable,
                contentPadding = contentPadding,
            )

            is NoticeDetailUiState.Success -> NoticeDetailContent(
                detail = state.detail,
                onOpenInBrowser = openLink,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * 通知详情内容。
 */
@Composable
private fun NoticeDetailContent(
    detail: NoticeDetail,
    onOpenInBrowser: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 标题
                Text(
                    detail.title,
                    style = MaterialTheme.typography.titleLarge,
                )

                // 发布日期
                Text(
                    detail.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // PDF 通知只给出下载入口
        detail.pdfUrlString?.let { pdfUrl ->
            item {
                Text(
                    text = pdfUrl,
                    // 打开 PDF 本身，而不是通知页——否则用户点进去还是同一个页面
                    modifier = Modifier.clickable { onOpenInBrowser(pdfUrl) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (detail.html.isNotBlank()) {
            item {
                // HTML 正文
                HtmlText(
                    text = detail.html,
                    onUriClick = onOpenInBrowser,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NoticeDetailContentPreview() {
    PomegranateExpressiveTheme {
        NoticeDetailContent(
            detail = NoticeDetail(
                title = "关于 2026 年端午节放假的通知",
                date = "2026-06-01",
                urlString = "https://jwc.gdufe.edu.cn/example",
                html = "<p>各教学单位：</p><p>根据学校安排，放假三天。</p>",
            ),
            onOpenInBrowser = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}
