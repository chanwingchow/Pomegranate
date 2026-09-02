package com.chow.pomegranate.ui.screen.cet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.SettingsVoice
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.afternoon
import com.chow.pomegranate.cet
import com.chow.pomegranate.details
import com.chow.pomegranate.morning
import com.chow.pomegranate.overview
import com.chow.pomegranate.service.cet.model.CetExam
import com.chow.pomegranate.service.cet.model.CetExamItem
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.conamobile.pdfkmp.viewer.PdfSource
import com.conamobile.pdfkmp.viewer.PdfViewer
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import kotlinx.coroutines.launch
import kotlinx.datetime.format.AmPmMarker
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * CET 页导航入口。
 */
fun EntryProviderScope<NavKey>.cetEntry() {
    entry<CetRoute> {
        CetScreen()
    }
}

/**
 * CET 页路由。
 */
@Serializable
data object CetRoute : NavKey

/**
 * CET 页。
 */
@Composable
private fun CetScreen(
    modifier: Modifier = Modifier,
) {
    CetContent(
        modifier = modifier,
    )
}

@Preview
@Composable
private fun CetContentPreview() {
    PomegranateExpressiveTheme {
        CetContent()
    }
}

@Composable
private fun CetContent(
    modifier: Modifier = Modifier,
    viewModel: CetViewModel = koinViewModel(),
) {
    val backStack = LocalBackStack.current

    // UI 状态
    val uiState by viewModel.uiState.collectAsState()

    val tabs = CetTab.entries

    val pagerState = rememberPagerState { tabs.size }

    // 高斯模糊
    val sky = rememberSky()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                tabs = tabs,
                pagerState = pagerState,
                isSuccessUiState = uiState is CetUiState.Success,
                onBack = {
                    backStack.removeLast()
                },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding + PaddingValues(16.dp)

        when (uiState) {
            is CetUiState.Loading -> {
                // 加载中
                LoadingContent(
                    contentPadding = contentPadding,
                )
            }

            is CetUiState.Error -> {
                // 加载失败
                ErrorContent(
                    throwable = (uiState as CetUiState.Error).throwable,
                    contentPadding = contentPadding,
                )
            }

            is CetUiState.Success -> {
                // 加载成功
                CetExamContent(
                    uiState = uiState as CetUiState.Success,
                    onLoadPdfBytes = {
                        viewModel.getCetExamPdf()
                    },
                    tabs = tabs,
                    pagerState = pagerState,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

/**
 * CET 标签。
 *
 * @property label 标签
 */
private enum class CetTab(
    val label: StringResource,
) {
    /**
     * 概览。
     */
    Overview(
        label = Res.string.overview,
    ),

    /**
     * 详情。
     */
    Details(
        label = Res.string.details,
    ),
}

/**
 * 顶部导航。
 */
@Composable
private fun TopBar(
    tabs: List<CetTab>,
    pagerState: PagerState,
    isSuccessUiState: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(0.8f),
        modifier = modifier,
    ) {
        Column(
            modifier = modifier,
        ) {
            PomTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.cet),
                    )
                },
                modifier = modifier,
                navigationIcon = {
                    // 返回按钮
                    BackIconButton(
                        onClick = onBack,
                    )
                },
                shadowElevation = 0.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            AnimatedVisibility(isSuccessUiState) {
                // 标签行
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(stringResource(tab.label))
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * CET 考试内容。
 */
@Composable
private fun CetExamContent(
    uiState: CetUiState.Success,
    onLoadPdfBytes: () -> Unit,
    tabs: List<CetTab>,
    pagerState: PagerState,
    contentPadding: PaddingValues,
) {
    LaunchedEffect(pagerState.currentPage) {
        val tab = tabs[pagerState.currentPage]

        if (tab == CetTab.Details) {
            // 加载 PDF
            onLoadPdfBytes()
        }
    }

    HorizontalPager(
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { page ->
        val tab = tabs[page]

        when (tab) {
            CetTab.Overview -> {
                // 概览
                CetOverview(
                    exam = uiState.exam,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                )
            }

            CetTab.Details -> {
                // 详情
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (val pdfUiState = uiState.pdfUiState) {
                        is CetUiState.CetPdfUiState.Idle, is CetUiState.CetPdfUiState.Loading -> {
                            // 空闲、加载中
                            LoadingContent(
                                contentPadding = contentPadding,
                            )
                        }

                        is CetUiState.CetPdfUiState.Error -> {
                            // 加载失败
                            ErrorContent(
                                throwable = pdfUiState.throwable,
                                contentPadding = contentPadding,
                            )
                        }

                        is CetUiState.CetPdfUiState.Success -> {
                            val pdfSource = remember(pdfUiState.pdfBytes) {
                                PdfSource.of(pdfUiState.pdfBytes)
                            }

                            // 加载成功
                            PdfViewer(
                                pdfSource,
                                shareFileName = "${stringResource(Res.string.cet)}.pdf",
                                backgroundColor = MaterialTheme.colorScheme.background,
                                contentPadding = contentPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CET 概览。
 */
@Composable
private fun CetOverview(
    exam: CetExam,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            // 考试名称
            Text(
                exam.name,
            )
        }

        item {
            // 英语四级笔试
            CetExamCard(
                examItem = exam.cet4Written,
                labelIcon = Icons.TwoTone.Edit,
            )
        }
        item {
            // 英语六级笔试
            CetExamCard(
                examItem = exam.cet6Written,
                labelIcon = Icons.TwoTone.Edit,
            )
        }
        item {
            // 英语四级口语
            CetExamCard(
                examItem = exam.cet4Speaking,
                labelIcon = Icons.TwoTone.SettingsVoice,
            )
        }
        item {
            // 英语六级口语
            CetExamCard(
                examItem = exam.cet6Speaking,
                labelIcon = Icons.TwoTone.SettingsVoice,
            )
        }
    }
}

/**
 * CET 考试卡片。
 */
@Composable
private fun CetExamCard(
    examItem: CetExamItem,
    labelIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = {
                Text(examItem.languages)
            },
            overlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 日期
                    Text("${examItem.date}")

                    // 上午/下午
                    if (examItem.amPmMarker != null) {
                        Text(
                            stringResource(
                                when (examItem.amPmMarker) {
                                    AmPmMarker.AM -> Res.string.morning
                                    AmPmMarker.PM -> Res.string.afternoon
                                },
                            ),
                        )
                    }
                }
            },
            trailingContent = {
                Icon(
                    labelIcon,
                    contentDescription = labelIcon.name,
                    modifier = Modifier.size(20.dp),
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                supportingContentColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}