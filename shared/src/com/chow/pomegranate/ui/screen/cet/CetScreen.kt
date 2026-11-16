package com.chow.pomegranate.ui.screen.cet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.service.cet.model.CetExamItem
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.conamobile.pdfkmp.viewer.PdfSource
import com.conamobile.pdfkmp.viewer.PdfViewer
import com.conamobile.pdfkmp.viewer.rememberPdfShareAction
import kotlinx.datetime.format.AmPmMarker
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object CetRoute : NavKey
@Serializable internal data class CetNoteRoute(val note: String) : NavKey
fun EntryProviderScope<NavKey>.cetEntry() {
    entry<CetRoute> { CetScreen() }
    entry<CetNoteRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { route -> CetNoteSheet(route.note) }
}

@Composable
private fun CetScreen(viewModel: CetViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val backStack = LocalBackStack.current
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val sharePdf = rememberPdfShareAction()
    // PDF 内部有同向横向滚动，将剩余位移和速度交给 Pager 的 scrollable 处理，避免中断吸附。
    val pageScrollConnection = remember { object : NestedScrollConnection {} }
    LaunchedEffect(pager.currentPage, (state as? CetUiState.Success)?.pdfUiState) {
        if (pager.currentPage == 1 && (state as? CetUiState.Success)?.pdfUiState == CetUiState.CetPdfUiState.Idle) viewModel.getCetExamPdf()
    }
    Scaffold(topBar = { StudyTopBar {
        PomTopAppBar(title = { Text("四六级") }, shadowElevation = 0.dp, navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
            actions = {
                (state as? CetUiState.Success)?.exam?.note?.takeIf { it.isNotBlank() }?.let { note ->
                    IconButton(onClick = { backStack.add(CetNoteRoute(note)) }) { Icon(Icons.Outlined.Info, "考试提示") }
                }
                IconButton(onClick = viewModel::getCetExam) { Icon(Icons.Rounded.Refresh, "刷新") }
            })
        StudyTabs(listOf("考试安排", "通知原文"), pager.currentPage) { index -> scope.launch { pager.animateScrollToPage(index) } }
    } }, floatingActionButton = {
        if (pager.currentPage == 0) BackToTopButton(gridState)
        else (state as? CetUiState.Success)?.let { current ->
            (current.pdfUiState as? CetUiState.CetPdfUiState.Success)?.let { pdf ->
                val fileName = current.exam.name.replace("考试时间", "CET考试时间") + ".pdf"
                SmallFloatingActionButton(onClick = { sharePdf(pdf.pdfBytes, fileName) }) {
                    Icon(Icons.Rounded.Share, "分享 PDF")
                }
            }
        }
    }) { padding ->
        PomPullToRefreshBox(refreshing && state is CetUiState.Success, viewModel::getCetExam, contentPadding = padding,
            enabled = state is CetUiState.Success) {
            when (val current = state) {
                CetUiState.Loading -> LoadingContent(contentPadding = padding)
                is CetUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorContent(current.throwable, Modifier.weight(1f)); TextButton(onClick = viewModel::getCetExam) { Text("重试") }
                }
                is CetUiState.Success -> HorizontalPager(pager, Modifier.fillMaxSize().padding(padding.withoutBottom()), verticalAlignment = Alignment.Top,
                    pageNestedScrollConnection = pageScrollConnection) { page ->
                    if (page == 0) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        val items = listOf("四级笔试" to current.exam.cet4Written, "六级笔试" to current.exam.cet6Written,
                            "四级口语" to current.exam.cet4Speaking, "六级口语" to current.exam.cet6Speaking)
                        LazyVerticalGrid(GridCells.Adaptive(300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                            Modifier.widthIn(max = 1100.dp).fillMaxSize(), state = gridState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + padding.calculateBottomPadding()),
                            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(current.exam.name, style = MaterialTheme.typography.titleMedium)
                            }
                            itemsIndexed(items) { index, (label, exam) -> CetCard(label, exam, index) }
                        }
                    } else when (val pdf = current.pdfUiState) {
                        CetUiState.CetPdfUiState.Idle, CetUiState.CetPdfUiState.Loading -> LoadingContent()
                        is CetUiState.CetPdfUiState.Error -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            ErrorContent(pdf.throwable, Modifier.weight(1f)); TextButton(onClick = viewModel::getCetExamPdf) { Text("重新加载原文") }
                        }
                        is CetUiState.CetPdfUiState.Success -> {
                            val source = remember(pdf.pdfBytes) { PdfSource.of(pdf.pdfBytes) }
                            PdfViewer(source, showShareButton = false, backgroundColor = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CetNoteSheet(note: String) {
    val backStack = LocalBackStack.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("考试提示", style = MaterialTheme.typography.titleLarge)
        SelectionContainer { Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun CetCard(label: String, exam: CetExamItem, index: Int) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpressiveIconBadge(if (index < 2) Icons.Rounded.Edit else Icons.Rounded.Mic, index, false, Modifier.size(36.dp), iconSize = 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val kind = label.takeLast(2)
                val name = exam.languages.trim().let { languages ->
                    when {
                        languages.isBlank() -> label
                        kind in languages -> languages
                        else -> "$languages$kind"
                    }
                }
                Text(name, style = MaterialTheme.typography.titleMedium)
                val period = exam.amPmMarker?.let { if (it == AmPmMarker.AM) "上午" else "下午" }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(exam.date.toString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    period?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
