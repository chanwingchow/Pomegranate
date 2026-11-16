package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.repository.ClassroomReplacements
import com.chow.pomegranate.repository.NameReplacementTarget

internal val LocalClassroomReplacements = staticCompositionLocalOf { ClassroomReplacements() }
internal val LocalCourseNameReplacements = staticCompositionLocalOf { ClassroomReplacements(target = NameReplacementTarget.Course) }

@Composable
internal fun classroomText(value: String, custom: String? = null): String = LocalClassroomReplacements.current.format(value, custom)

@Composable
internal fun courseNameText(value: String, custom: String? = null): String = LocalCourseNameReplacements.current.format(value, custom)

@Composable
internal fun studyTitleText(detail: StudyDetailRoute): String =
    if (listOf("plan:", "timetable:", "select:", "selected:", "selected-table:").any { detail.id.startsWith(it) }) courseNameText(detail.title)
    else detail.title

internal val classroomFieldLabels = setOf("教室", "原教室", "考场", "考试地点", "上课地点", "地点", "办公室", "办公地点", "科室")

/** 详情保留替换前的原文，复制任一段文字时也能区分显示值与原始值。 */
@Composable
internal fun ClassroomDetailValue(value: String, modifier: Modifier = Modifier, original: String = value, label: String = "教室",
    schedule: Boolean = false, applyReplacements: Boolean = true) {
    val rules = LocalClassroomReplacements.current
    val display = if (!applyReplacements) value else if (schedule) rules.formatSchedule(value) else rules.format(value)
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(display, style = MaterialTheme.typography.bodyLarge)
            if (display != original) Text("原始$label：$original", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
