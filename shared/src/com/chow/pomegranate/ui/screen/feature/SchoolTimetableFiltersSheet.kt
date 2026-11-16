package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.chow.pomegranate.service.academic.affairs.model.Section
import com.chow.pomegranate.ui.component.PomTextField
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.component.LocalClassroomReplacements
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.serialization.Serializable

@Serializable
data class SchoolTimetableFiltersRoute(val filters: SchoolTimetableFilters, val options: SchoolTimetableFilterOptions) : NavKey

data class SchoolTimetableFilterResult(val filters: SchoolTimetableFilters)

@Composable
internal fun SchoolTimetableFiltersSheet(route: SchoolTimetableFiltersRoute) {
    val backStack = LocalBackStack.current
    val results = LocalResultEventBus.current
    var teacher by rememberSaveable { mutableStateOf(route.filters.teacher) }
    var classroom by rememberSaveable { mutableStateOf(route.filters.classroom) }
    var clazz by rememberSaveable { mutableStateOf(route.filters.clazz) }
    var weekday by rememberSaveable { mutableStateOf(route.filters.weekday) }
    var sectionName by rememberSaveable { mutableStateOf(route.filters.section?.name) }
    var week by rememberSaveable { mutableStateOf(route.filters.week) }
    val weekdays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    Column(Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("筛选课表", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChoice("星期", weekday?.let { weekdays[it - 1] } ?: "不限星期", listOf("不限星期") + weekdays,
                    onSelect = { weekday = it.takeIf { it > 0 } })
                FilterChoice("节次", sectionName?.let { "${Section.valueOf(it)} 节" } ?: "不限节次",
                    listOf("不限节次") + Section.entries.map { "$it 节" }, onSelect = { sectionName = Section.entries.getOrNull(it - 1)?.name })
                FilterChoice("周次", week?.let { "第 $it 周" } ?: "不限周次", listOf("不限周次") + (1..route.options.maxWeek).map { "第 $it 周" },
                    onSelect = { week = it.takeIf { it > 0 } })
            }
            SchoolFilterText("教师", teacher, route.options.teachers) { teacher = it }
            SchoolFilterText("教室", classroom, route.options.classrooms) { classroom = it }
            SchoolFilterText("班级", clazz, route.options.classes) { clazz = it }
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { teacher = ""; classroom = ""; clazz = ""; weekday = null; sectionName = null; week = null }) { Text("重置") }
            Button(onClick = {
                results.sendResult(SchoolTimetableFilterResult(SchoolTimetableFilters(teacher, classroom, clazz, weekday,
                    sectionName?.let(Section::valueOf), week).normalized()))
                backStack.removeLastOrNull()
                // 旧详情包含未筛选的课次，应用后先回到列表，重新选择时只展示匹配课次。
                val detail = backStack.lastOrNull() as? StudyDetailRoute
                if (detail?.id?.startsWith("timetable:") == true) backStack.removeLastOrNull()
            }, Modifier.weight(1f)) { Text("应用筛选") }
        }
    }
}

@Composable
private fun SchoolFilterText(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rules = LocalClassroomReplacements.current
    fun display(option: String) = if (label == "教室") rules.format(option) else option
    Box(Modifier.fillMaxWidth()) {
        PomTextField(value, onChange, Modifier.fillMaxWidth(), label = label, placeholder = "输入或选择$label",
            supportingText = if (display(value) != value) "显示为：${display(value)}" else null,
            trailingIcon = if (options.isNotEmpty()) ({
                IconButton(onClick = { expanded = true }) { Icon(Icons.Rounded.ExpandMore, "选择$label") }
            }) else null)
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 280.dp).widthIn(min = 200.dp, max = 360.dp)) {
            DropdownMenuItem(text = { Text("不限$label") }, onClick = { onChange(""); expanded = false })
            options.filter { value.isBlank() || value.trim() in options || it.contains(value.trim(), ignoreCase = true) || display(it).contains(value.trim(), ignoreCase = true) ||
                label == "教室" && rules.matches(it, value.trim()) }.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}
