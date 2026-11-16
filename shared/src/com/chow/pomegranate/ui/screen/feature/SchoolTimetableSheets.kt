package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import com.chow.pomegranate.repository.SchoolTimetableRepository
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

@Serializable data class SchoolSemesterRoute(val selected: String) : NavKey
@Serializable data object SchoolTimetableDataRoute : NavKey
@Serializable data class SchoolTimetableDeleteRoute(val owner: String, val semester: String) : NavKey
internal data class SchoolSemesterSelection(val semester: String)
private data class SchoolDataFeedback(val text: String)

@KoinViewModel
class SchoolTimetableSheetsViewModel(private val repository: SchoolTimetableRepository) : ViewModel() {
    val owner = repository.owner()
    val saved = repository.savedByOwner.map { it[owner].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val semesters: StateFlow<List<Semester>>
        field = MutableStateFlow(emptyList())
    val loading: StateFlow<Boolean>
        field = MutableStateFlow(false)
    val error: StateFlow<Boolean>
        field = MutableStateFlow(false)
    val busy: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init { viewModelScope.launch { repository.readSaved() } }

    fun loadSemesters(selected: String) {
        if (loading.value || semesters.value.isNotEmpty()) return
        loading.value = true
        viewModelScope.launch {
            try {
                semesters.value = repository.semesters(Semester.parse(selected))
                error.value = false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error.value = true }
            finally { loading.value = false }
        }
    }
    fun delete(owner: String, semester: String, onResult: (Boolean) -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { repository.delete(owner, Semester.parse(semester)); onResult(true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onResult(false) }
            finally { busy.value = false }
        }
    }
}

@Composable
private fun schoolSheetHeight() = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .8f }

@Composable
internal fun SchoolSemesterSheet(route: SchoolSemesterRoute, viewModel: SchoolTimetableSheetsViewModel = koinViewModel()) {
    val stack = LocalBackStack.current
    val results = LocalResultEventBus.current
    val semesters by viewModel.semesters.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(route.selected) { viewModel.loadSemesters(route.selected) }
    val current = Semester.now()
    val fallback = remember(current, route.selected) {
        (current.year - 5..current.year).flatMap { year -> Semester.Term.entries.map { Semester(year, it) } }
            .filter { it <= current.next() } + Semester.parse(route.selected)
    }
    val options = (semesters.ifEmpty { fallback } + saved.map { it.timetable.semester }).distinct().sortedDescending()
    Column(Modifier.fillMaxWidth().heightIn(max = schoolSheetHeight())) {
        Text("切换学期", Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp), style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            if (loading) item { Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { PomLoadingIndicator() } }
            if (error) item { TextButton(onClick = { viewModel.loadSemesters(route.selected) }) { Text("学期列表加载失败，重试") } }
            items(options, key = { it.toString() }) { semester ->
                Surface(onClick = {
                    results.sendResult(SchoolSemesterSelection(semester.toString()))
                    stack.removeLastOrNull()
                }, shape = MaterialTheme.shapes.medium,
                    color = if (semester.toString() == route.selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(semester.toString(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (semester == current) Text("本学期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (semester.toString() == route.selected) Icon(Icons.Rounded.Check, "当前学期")
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
internal fun SchoolTimetableDataSheet(viewModel: SchoolTimetableSheetsViewModel = koinViewModel()) {
    val saved by viewModel.saved.collectAsState()
    val stack = LocalBackStack.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ResultEffect<SchoolDataFeedback> { result -> scope.launch { snackbar.showSnackbar(result.text) } }
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = schoolSheetHeight())) {
            Text("本地课表数据", Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp), style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (saved.isEmpty()) item { EmptyContent(Modifier.fillMaxWidth().height(200.dp), message = "暂无本地课表") }
                items(saved, key = { it.timetable.semester.toString() }) { entry ->
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.timetable.semester.toString(), style = MaterialTheme.typography.titleMedium)
                                Text("${entry.timetable.courses.map { it.name }.distinct().size} 门课程 · ${entry.timetable.courses.size} 个课次",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { stack.add(SchoolTimetableDeleteRoute(viewModel.owner, entry.timetable.semester.toString())) }) {
                                Icon(Icons.Rounded.DeleteOutline, "删除 ${entry.timetable.semester} 的本地课表", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun SchoolTimetableDeleteDialog(route: SchoolTimetableDeleteRoute, viewModel: SchoolTimetableSheetsViewModel = koinViewModel()) {
    val busy by viewModel.busy.collectAsState()
    val stack = LocalBackStack.current
    val results = LocalResultEventBus.current
    AlertDialog(onDismissRequest = { if (!busy) stack.removeLastOrNull() },
        title = { Text("删除本地课表？") }, text = { Text("${route.semester} 的课表将从本机移除，需要时可重新下载。") },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            viewModel.delete(route.owner, route.semester) { success ->
                stack.removeLastOrNull()
                results.sendResult(SchoolDataFeedback(if (success) "已删除本地课表" else "删除失败，请重试"))
            }
        }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { stack.removeLastOrNull() }) { Text("取消") } })
}
