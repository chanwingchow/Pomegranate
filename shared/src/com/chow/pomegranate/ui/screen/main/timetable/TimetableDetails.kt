package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

@Serializable data class TimetableNotesRoute(val note: String) : NavKey
@Serializable data class TimetableCourseRoute(val detail: StudyDetailRoute, val lessons: List<TimetableLesson> = emptyList(), val week: Int = 0) : NavKey
@Serializable data class TimetableEditRoute(val lesson: TimetableLesson, val customization: TimetableCourseCustomization? = null) : NavKey

internal fun timetableCourseRoute(timetable: com.chow.pomegranate.service.academic.affairs.model.Timetable,
    courses: List<com.chow.pomegranate.service.academic.affairs.model.TimetableCourse>, week: Int, row: Int, day: Int): TimetableCourseRoute {
    val lessons = courses.map { it.lesson(timetable.ownerKey()) }
    return TimetableCourseRoute(StudyDetailRoute("timetable:$week:$row:$day",
        if (courses.size == 1) courses.first().name else "同一时段的 ${courses.size} 门课程",
        "星期${"一二三四五六日"[day]} · ${row * 2 + 1}-${row * 2 + 2} 节"), lessons, week)
}

@Composable
internal fun TimetableNotesSheet(route: TimetableNotesRoute) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("课表备注", style = MaterialTheme.typography.titleLarge)
        SelectionContainer { Text(route.note.ifBlank { "暂无备注" }, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
internal fun TimetableCourseSheet(route: TimetableCourseRoute) {
    val backStack = LocalBackStack.current
    val repository = koinInject<TimetableRepository>()
    val customizationRepository = koinInject<TimetableCustomizationRepository>()
    val current by repository.current.collectAsState()
    val preferences = koinInject<AppPreferenceRepository>()
    val campusOrdinal by remember(preferences) { preferences.observe(com.chow.pomegranate.data.local.PreferenceKeys.CAMPUS) }.collectAsState(initial = null)
    val campus = com.chow.pomegranate.service.foundation.Campus.entries.getOrNull(campusOrdinal?.toIntOrNull() ?: -1)
        ?: com.chow.pomegranate.service.foundation.Campus.Canton
    val owner = route.lessons.firstOrNull()?.owner
    val customizations by remember(owner) { owner?.let(customizationRepository::observe) ?: kotlinx.coroutines.flow.flowOf(emptyMap()) }
        .collectAsState(initial = emptyMap())
    val pane = LocalTimetableDetailPane.current
    Column(Modifier.fillMaxSizeIf(pane).verticalScroll(rememberScrollState())
        .then(if (pane) Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End)) else Modifier)
        .padding(horizontal = 24.dp).padding(top = if (pane) 16.dp else 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (pane) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("课程详情", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { backStack.removeLastOrNull() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, "收起课程详情", Modifier.size(20.dp))
            }
        }
        if (route.lessons.size != 1) Text(route.detail.title, style = MaterialTheme.typography.titleLarge)
        if (route.lessons.isEmpty()) {
            route.detail.sections.forEach { StudySectionContent(it) }
        }
        route.lessons.forEach { snapshot ->
            val lesson = current?.takeIf { it.ownerKey() == snapshot.owner }?.courses
                ?.map { it.lesson(snapshot.owner) }?.firstOrNull { it.key == snapshot.key } ?: snapshot
            val customization = customizations[lesson.key]
            val original = com.chow.pomegranate.service.academic.affairs.model.TimetableCourse(lesson.name, lesson.teacher,
                lesson.weeks, lesson.classroom, kotlinx.datetime.DayOfWeek.entries[lesson.day], lesson.sections.toSet(), lesson.clazz)
            if (!original.isInWeek(route.week)) Text("非本周课程", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(courseNameText(lesson.name, customization?.name), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { backStack.add(TimetableEditRoute(lesson, customization)) }) { Icon(Icons.Outlined.Edit, "修改课程信息") }
            }
            if (courseNameText(lesson.name, customization?.name) != lesson.name) Text("原始名称：${lesson.name}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("星期${"一二三四五六日"[lesson.day]} · ${lesson.sections.joinToString("、")} 节",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val time = timetableTimeFor(lesson.sections, campus)
                if (time != null) DetailRow(StudyField("时间", time), Icons.Outlined.Schedule)
                DetailRow(StudyField("教室", (customization?.classroom ?: lesson.classroom.orEmpty()).ifBlank { "未设置" },
                    originalValue = lesson.classroom.orEmpty().ifBlank { "未设置" }, applyReplacements = customization?.classroom == null), Icons.Outlined.LocationOn)
                lesson.teacher?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(StudyField("教师", it, teacherQuery = it), Icons.Outlined.PersonOutline)
                }
                DetailRow(StudyField("周次", lesson.weeks), Icons.Outlined.DateRange)
                lesson.clazz?.takeIf { it.isNotBlank() }?.let { DetailRow(StudyField("班级", it), Icons.Outlined.Groups) }
            }
            customization?.note?.takeIf { it.isNotBlank() }?.let { note ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Notes, null, Modifier.size(20.dp)); Text("课程备注", style = MaterialTheme.typography.titleSmall)
                        }
                        SelectionContainer { Text(note, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
            if (route.lessons.size > 1) HorizontalDivider()
        }
        if (pane) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private fun Modifier.fillMaxSizeIf(pane: Boolean): Modifier = if (pane) fillMaxSize() else fillMaxWidth()

@Composable
private fun DetailRow(field: StudyField, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    if (largeText) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp)); Text(field.label, style = MaterialTheme.typography.labelLarge)
        }
        StudyFieldValue(field, Modifier.padding(start = 28.dp))
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = if (field.label in classroomFieldLabels) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(field.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StudyFieldValue(field, Modifier.weight(2.4f))
    }
}

@KoinViewModel
class TimetableEditViewModel(private val repository: TimetableCustomizationRepository) : ViewModel() {
    fun save(lesson: TimetableLesson, name: String, classroom: String, note: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try { repository.save(lesson, classroom, note, name); onResult(null) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onResult(error) }
        }
    }
}

@Composable
internal fun TimetableEditSheet(route: TimetableEditRoute, viewModel: TimetableEditViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val snackbar = LocalSnackbarHostState.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(route.customization?.name ?: route.lesson.name) }
    var classroom by rememberSaveable { mutableStateOf(route.customization?.classroom ?: route.lesson.classroom.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(route.customization?.note.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("修改课程信息", style = MaterialTheme.typography.titleLarge)
            PomTextField(name, { name = it }, Modifier.fillMaxWidth(), label = "课程名称", placeholder = "输入课程名称或常用简称",
                singleLine = false, enabled = !saving, isError = name.isBlank(),
                supportingText = if (name.isBlank()) "课程名称不能为空" else "可填写常用简称")
            val editedRoom = classroom.trim().takeUnless { it == route.lesson.classroom.orEmpty().trim() }
            val displayRoom = classroomText(classroom, editedRoom)
            PomTextField(classroom, { classroom = it }, Modifier.fillMaxWidth(), label = "教室", placeholder = "例如：实验楼 A301", enabled = !saving,
                supportingText = if (displayRoom != classroom) "显示为：$displayRoom" else null)
            PomTextField(note, { note = it }, Modifier.fillMaxWidth(), label = "课程备注", placeholder = "添加需要记住的事项", singleLine = false, enabled = !saving)
            Text("仅保存到本机，刷新课表后仍保留。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (route.customization != null) TextButton(onClick = { name = route.lesson.name; classroom = route.lesson.classroom.orEmpty(); note = "" }, enabled = !saving) {
                Text("恢复教务信息")
            }
        }
        Button(onClick = {
            saving = true
            viewModel.save(route.lesson, name, classroom, note) { failure ->
                saving = false
                if (failure == null) backStack.removeLastOrNull()
                scope.launch { snackbar.showSnackbar(if (failure == null) "课程信息已保存。" else "保存失败：${failure.message ?: "请重试。"}") }
            }
        }, enabled = !saving && name.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            if (saving) PomLoadingIndicator(size = InlineLoadingIndicatorSize) else Text("保存")
        }
    }
}
