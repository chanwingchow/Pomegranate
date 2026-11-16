package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.chow.pomegranate.repository.CourseSearchFilters
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.Section
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.ui.component.PomTextField
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.serialization.Serializable

@Serializable
data class CourseFiltersRoute(val category: CourseSelectCategory, val filters: CourseSearchFilters) : NavKey

data class CourseFilterSelection(val category: CourseSelectCategory, val filters: CourseSearchFilters)

private val weekdayLabels = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

internal fun CourseSearchFilters.summary(category: CourseSelectCategory): String {
    if (category == CourseSelectCategory.Basic || category == CourseSelectCategory.Optional) return "按名称或教师搜索"
    return buildList {
        if (teacher.isNotBlank()) add(teacher)
        weekday?.let { add(weekdayLabels[it - 1]) }
        section?.let { add("${it} 节") }
        if (category == CourseSelectCategory.General) campus?.let { add(if (it == Campus.Canton) "广州校区" else "佛山校区") }
        if (filterFull) add("有余位")
        if (filterConflicts) add("不冲突")
    }.joinToString(" · ").ifBlank { "全部条件" }
}

@Composable
internal fun CourseFiltersSheet(route: CourseFiltersRoute) {
    val backStack = LocalBackStack.current
    val results = LocalResultEventBus.current
    var categoryName by rememberSaveable { mutableStateOf(route.category.name) }
    var teacher by rememberSaveable { mutableStateOf(route.filters.teacher) }
    var weekday by rememberSaveable { mutableStateOf(route.filters.weekday) }
    var sectionName by rememberSaveable { mutableStateOf(route.filters.section?.name) }
    var campusName by rememberSaveable { mutableStateOf(route.filters.campus?.name) }
    var full by rememberSaveable { mutableStateOf(route.filters.filterFull) }
    var conflicts by rememberSaveable { mutableStateOf(route.filters.filterConflicts) }
    val category = CourseSelectCategory.valueOf(categoryName)
    val extended = category != CourseSelectCategory.Basic && category != CourseSelectCategory.Optional
    Column(Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("筛选课程", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("课程类别", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CourseSelectCategory.entries.forEach { item ->
                        FilterChip(category == item, onClick = { categoryName = item.name }, label = { Text(item.label) })
                    }
                }
            }
            if (extended) {
                PomTextField(teacher, { teacher = it }, Modifier.fillMaxWidth(), label = "教师或课程名称", placeholder = "输入教师姓名或课程名称")
                // FlowRow 会在大字体和窄屏自动换行，选择器没有固定高度。
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChoice("上课日期", weekday?.let { weekdayLabels[it - 1] } ?: "不限星期",
                        listOf("不限星期") + weekdayLabels, onSelect = { weekday = it.takeIf { it > 0 } })
                    FilterChoice("上课节次", sectionName?.let { "${Section.valueOf(it)} 节" } ?: "不限节次",
                        listOf("不限节次") + Section.entries.map { "$it 节" },
                        onSelect = { sectionName = Section.entries.getOrNull(it - 1)?.name })
                }
                if (category == CourseSelectCategory.General) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("校区", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, Campus.Canton, Campus.Foshan).forEach { campus ->
                            FilterChip(campusName == campus?.name, onClick = { campusName = campus?.name },
                                label = { Text(when (campus) { Campus.Canton -> "广州校区"; Campus.Foshan -> "佛山校区"; null -> "不限" }) })
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterSwitch("只看有余位", full) { full = !full }
                    FilterSwitch("只看不冲突", conflicts) { conflicts = !conflicts }
                }
            } else Text("此类别支持按课程名称或教师搜索，暂不提供其他筛选。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                categoryName = CourseSelectCategory.General.name
                teacher = ""; weekday = null; sectionName = null; campusName = null; full = true; conflicts = true
            }) { Text("重置") }
            Button(onClick = {
                val filters = CourseSearchFilters(teacher, weekday, sectionName?.let(Section::valueOf), campusName?.let(Campus::valueOf), full, conflicts)
                    .forCategory(category)
                results.sendResult(CourseFilterSelection(category, filters))
                backStack.removeLastOrNull()
            }, modifier = Modifier.weight(1f)) { Text("应用筛选") }
        }
    }
}

@Composable
internal fun FilterChoice(label: String, value: String, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            FilledTonalButton(onClick = { expanded = true }) {
                Text(value); Spacer(Modifier.width(8.dp)); Icon(Icons.Rounded.ExpandMore, null)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(index) })
                }
            }
        }
    }
}

@Composable
private fun FilterSwitch(label: String, checked: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.semantics { role = Role.Switch; toggleableState = ToggleableState(checked) },
        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked, onCheckedChange = null)
        }
    }
}
