package com.chow.pomegranate.ui.screen.transcript

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.assessment
import com.chow.pomegranate.close
import com.chow.pomegranate.course_attribute
import com.chow.pomegranate.course_category
import com.chow.pomegranate.course_id
import com.chow.pomegranate.level_score_search_hint
import com.chow.pomegranate.level_score_search
import com.chow.pomegranate.course_score_search_hint
import com.chow.pomegranate.course_score_search
import com.chow.pomegranate.course_transcript
import com.chow.pomegranate.credits
import com.chow.pomegranate.elective_category
import com.chow.pomegranate.exam_category
import com.chow.pomegranate.exam_date
import com.chow.pomegranate.final_score
import com.chow.pomegranate.hours
import com.chow.pomegranate.lab_score
import com.chow.pomegranate.level_transcript
import com.chow.pomegranate.load_failed
import com.chow.pomegranate.no_course_transcript
import com.chow.pomegranate.no_academic_data
import com.chow.pomegranate.no_level_transcript
import com.chow.pomegranate.note
import com.chow.pomegranate.overview
import com.chow.pomegranate.regular_score
import com.chow.pomegranate.refresh
import com.chow.pomegranate.score
import com.chow.pomegranate.score_mark
import com.chow.pomegranate.semester
import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.academic.affairs.model.LevelGrade
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.transcript
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.component.NotLoggedInContent
import com.chow.pomegranate.ui.screen.login.LoginRoute

/**
 * 成绩页导航入口。
 */
fun EntryProviderScope<NavKey>.transcriptEntry() {
    entry<TranscriptRoute> { TranscriptScreen() }
}

/**
 * 成绩页路由。
 */
@Serializable
data object TranscriptRoute : NavKey

/**
 * 成绩页。
 */
@Composable
private fun TranscriptScreen(
    modifier: Modifier = Modifier,
    viewModel: TranscriptViewModel = koinViewModel(),
) {
    TranscriptContent(
        uiState = viewModel.uiState.collectAsState().value,
        onRefresh = viewModel::refresh,
        onSubmitCourseKeyword = viewModel::submitCourseKeyword,
        onSubmitLevelKeyword = viewModel::submitLevelKeyword,
        modifier = modifier,
    )
}

/**
 * 成绩页内容。
 */
@Composable
private fun TranscriptContent(
    uiState: TranscriptUiState,
    onRefresh: () -> Unit,
    onSubmitCourseKeyword: (String) -> Unit,
    onSubmitLevelKeyword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val loadFailedMessage = stringResource(Res.string.load_failed)
    val tabs = TranscriptTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val sky = rememberSky()
    var showOverview by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        val error = uiState as? TranscriptUiState.Error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.throwable.message ?: loadFailedMessage)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                tabs = tabs,
                pagerState = pagerState,
                onBack = { backStack.removeLastOrNull() },
                onShowOverview = { showOverview = true },
                onRefresh = onRefresh,
                showOverview = uiState is TranscriptUiState.Success,
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding + PaddingValues(8.dp)
        when (uiState) {
            is TranscriptUiState.NotLoggedIn -> {
                // 未登录，不请求受保护接口，直接引导登录
                NotLoggedInContent(
                    onLogin = { backStack.add(LoginRoute) },
                    contentPadding = contentPadding,
                )
            }

            TranscriptUiState.Loading -> LoadingContent(contentPadding = contentPadding)
            is TranscriptUiState.Error -> ErrorContent(
                throwable = uiState.throwable,
                contentPadding = contentPadding,
            )
            is TranscriptUiState.Success -> TranscriptSuccessContent(
                courseTranscript = uiState.transcripts.course,
                levelTranscript = uiState.transcripts.level,
                courseKeyword = uiState.courseKeyword,
                levelKeyword = uiState.levelKeyword,
                onSubmitCourseKeyword = onSubmitCourseKeyword,
                onSubmitLevelKeyword = onSubmitLevelKeyword,
                tabs = tabs,
                pagerState = pagerState,
                sky = sky,
                contentPadding = contentPadding,
            )
        }
    }

    val courseTranscript = (uiState as? TranscriptUiState.Success)?.transcripts?.course
    if (showOverview && courseTranscript != null) {
        OverviewDialog(
            overview = courseTranscript.overview,
            onDismiss = { showOverview = false },
        )
    }
}

/**
 * 成绩页标签。
 *
 * @property label 标签字符串资源
 */
private enum class TranscriptTab(val label: StringResource) {
    /** 课程成绩。 */
    CourseTranscript(Res.string.course_transcript),

    /** 等级成绩。 */
    LevelTranscript(Res.string.level_transcript),
}

/**
 * 成绩页顶部导航。
 */
@Composable
private fun TopBar(
    tabs: List<TranscriptTab>,
    pagerState: PagerState,
    onBack: () -> Unit,
    onShowOverview: () -> Unit,
    onRefresh: () -> Unit,
    showOverview: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(0.8f),
        modifier = modifier,
    ) {
        Column {
            PomTopAppBar(
                title = { Text(stringResource(Res.string.transcript)) },
                navigationIcon = { BackIconButton(onClick = onBack) },
                actions = {
                    if (showOverview && tabs[pagerState.currentPage] == TranscriptTab.CourseTranscript) {
                        IconButton(onClick = onShowOverview) {
                            Icon(
                                Icons.TwoTone.Info,
                                contentDescription = stringResource(Res.string.overview),
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                        )
                    }
                },
                shadowElevation = 0.dp,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.label)) },
                    )
                }
            }
        }
    }
}

/**
 * 成绩页加载成功后的主体内容。
 */
@Composable
private fun TranscriptSuccessContent(
    courseTranscript: CourseTranscript,
    levelTranscript: LevelTranscript,
    courseKeyword: String,
    levelKeyword: String,
    onSubmitCourseKeyword: (String) -> Unit,
    onSubmitLevelKeyword: (String) -> Unit,
    sky: Sky,
    contentPadding: PaddingValues,
    tabs: List<TranscriptTab> = TranscriptTab.entries,
    pagerState: PagerState = rememberPagerState { tabs.size },
) {
    HorizontalPager(
        state = pagerState,
        // 顶栏的毛玻璃要采样这里：成绩列表得铺满内容区并在顶栏下方延伸
        modifier = Modifier.fillMaxSize().sky(sky = sky),
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (tabs[page]) {
            TranscriptTab.CourseTranscript -> CourseTranscriptContent(
                transcript = courseTranscript,
                keyword = courseKeyword,
                onSubmitKeyword = onSubmitCourseKeyword,
                contentPadding = contentPadding,
            )

            TranscriptTab.LevelTranscript -> LevelTranscriptContent(
                transcript = levelTranscript,
                keyword = levelKeyword,
                onSubmitKeyword = onSubmitLevelKeyword,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * 课程成绩列表。
 */
@Composable
private fun CourseTranscriptContent(
    transcript: CourseTranscript,
    keyword: String,
    onSubmitKeyword: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var selectedGrade by remember { mutableStateOf<CourseGrade?>(null) }
    var draftQuery by androidx.compose.runtime.saveable.rememberSaveable(keyword) { mutableStateOf(keyword) }
    val grades = transcript.items.filterCourseGradesByKeyword(keyword)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(240.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = draftQuery,
                onValueChange = { draftQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.course_score_search)) },
                placeholder = { Text(stringResource(Res.string.course_score_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitKeyword(draftQuery) }),
            )
        }
        if (grades.isEmpty()) {
            item { EmptyText(if (keyword.isBlank()) Res.string.no_course_transcript else Res.string.no_academic_data) }
        } else {
            items(grades, key = CourseGrade::courseId) { grade ->
                CourseGradeCard(grade = grade, onClick = { selectedGrade = grade })
            }
        }
    }
    selectedGrade?.let { grade ->
        CourseGradeDialog(grade = grade, onDismiss = { selectedGrade = null })
    }
}

/**
 * 课程成绩卡片。
 */
@Composable
private fun CourseGradeCard(
    grade: CourseGrade,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    grade.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    grade.score,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(grade.semester.toString(), style = MaterialTheme.typography.labelMedium)
                Text(
                    "${grade.credits} ${stringResource(Res.string.credits)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * 课程成绩详情弹窗。
 */
@Composable
private fun CourseGradeDialog(grade: CourseGrade, onDismiss: () -> Unit) {
    // 详情项
    val details = listOf(
        Res.string.score to grade.score,
        Res.string.semester to grade.semester.toString(),
        Res.string.course_id to grade.courseId,
        Res.string.credits to grade.credits.toString(),
        Res.string.hours to grade.hours.toString(),
        Res.string.regular_score to grade.regularScore,
        Res.string.lab_score to grade.labScore,
        Res.string.final_score to grade.finalScore,
        Res.string.assessment to grade.assessment,
        Res.string.course_attribute to grade.attribute,
        Res.string.course_category to grade.category,
        Res.string.elective_category to grade.electiveCategory,
        Res.string.exam_category to grade.examCategory,
        Res.string.score_mark to grade.mark,
        Res.string.note to grade.note,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(grade.name) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(details) { (label, value) ->
                    DetailRow(label, value)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        },
    )
}

/**
 * 等级成绩列表。
 */
@Composable
private fun LevelTranscriptContent(
    transcript: LevelTranscript,
    keyword: String,
    onSubmitKeyword: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var selectedGrade by remember { mutableStateOf<LevelGrade?>(null) }
    var draftQuery by androidx.compose.runtime.saveable.rememberSaveable(keyword) { mutableStateOf(keyword) }
    val grades = transcript.items.filterLevelGradesByKeyword(keyword)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(240.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = draftQuery,
                onValueChange = { draftQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.level_score_search)) },
                placeholder = { Text(stringResource(Res.string.level_score_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitKeyword(draftQuery) }),
            )
        }
        if (grades.isEmpty()) {
            item { EmptyText(if (keyword.isBlank()) Res.string.no_level_transcript else Res.string.no_academic_data) }
        } else {
            items(grades, key = { "${it.name}-${it.date}" }) { grade ->
                LevelGradeCard(grade = grade, onClick = { selectedGrade = grade })
            }
        }
    }
    selectedGrade?.let { grade ->
        LevelGradeDialog(grade = grade, onDismiss = { selectedGrade = null })
    }
}

/**
 * 等级成绩卡片。
 */
@Composable
private fun LevelGradeCard(grade: LevelGrade, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    grade.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    grade.score,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(grade.date.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * 等级成绩详情弹窗。
 */
@Composable
private fun LevelGradeDialog(grade: LevelGrade, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(grade.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(Res.string.score, grade.score)
                DetailRow(Res.string.exam_date, grade.date.toString())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        },
    )
}

/**
 * 详情字段行；字段值为空时不显示。
 */
@Composable
private fun DetailRow(label: StringResource, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(label),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, modifier = Modifier.weight(2f))
        }
    }
}

/**
 * 成绩列表为空时的提示。
 */
@Composable
private fun EmptyText(label: StringResource) {
    Text(
        stringResource(label),
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 课程成绩概览弹窗。
 */
@Composable
private fun OverviewDialog(
    overview: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.overview)) },
        text = { Text(overview) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Preview
@Composable
private fun TranscriptSuccessContentPreview() {
    PomegranateExpressiveTheme {
        TranscriptSuccessContent(
            courseTranscript = CourseTranscript(
                userId = "preview",
                overview = "平均绩点 3.72 · 已修学分 84.5",
                items = listOf(
                    CourseGrade(
                        semester = Semester(2025, Semester.Term.First),
                        courseId = "CS301",
                        name = "操作系统",
                        regularScore = "92",
                        labScore = "95",
                        finalScore = "88",
                        score = "90",
                        credits = 3.0,
                        hours = 48.0,
                        assessment = "考试",
                        attribute = "必修",
                        category = "专业核心课",
                        electiveCategory = null,
                        examCategory = "正常考试",
                        mark = "优秀",
                        note = null,
                    ),
                ),
            ),
            levelTranscript = LevelTranscript(
                userId = "preview",
                items = listOf(
                    LevelGrade(
                        name = "全国大学英语四级考试",
                        score = "562",
                        date = LocalDate(2025, 6, 14),
                    ),
                ),
            ),
            courseKeyword = "",
            levelKeyword = "",
            onSubmitCourseKeyword = {},
            onSubmitLevelKeyword = {},
            sky = rememberSky(),
            contentPadding = PaddingValues(16.dp),
        )
    }
}
