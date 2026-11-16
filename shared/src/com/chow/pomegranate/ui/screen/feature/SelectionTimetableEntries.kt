package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse

internal data class SelectionTimetableEntry(val text: String, val course: SelectedCourse?) {
    val title: String get() = course?.name ?: text.lineSequence().first()
    val supporting: String? get() = course?.classroom ?: text.lines().drop(1).joinToString("\n").takeIf(String::isNotBlank)
}

/** 保留同一格的多门课；只在唯一匹配已选课程时提供该课程的操作。 */
internal fun selectionTimetableEntries(text: String, courses: List<SelectedCourse>): List<SelectionTimetableEntry> {
    fun matches(line: String): List<SelectedCourse> {
        val candidates = courses.filter { course ->
            course.name.isNotBlank() && (line == course.name || line.startsWith(course.name) &&
                line.getOrNull(course.name.length)?.let { it in " （([【\t" } == true)
        }
        val longest = candidates.maxOfOrNull { it.name.length }
        return candidates.filter { it.name.length == longest }
    }
    val result = mutableListOf<SelectionTimetableEntry>()
    val lines = mutableListOf<String>()
    fun flush() {
        if (lines.isEmpty()) return
        result += SelectionTimetableEntry(lines.joinToString("\n"), matches(lines.first()).singleOrNull())
        lines.clear()
    }
    text.lines().forEach { raw ->
        val line = raw.trim()
        if (line.isBlank()) flush()
        else {
            if (matches(line).isNotEmpty()) flush()
            lines += line
        }
    }
    flush()
    return result
}
