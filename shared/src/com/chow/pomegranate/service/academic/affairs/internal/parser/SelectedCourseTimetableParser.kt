package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.SelectedCourseTimetable
import com.fleeksoft.ksoup.Ksoup

internal object SelectedCourseTimetableParser {
    fun parse(html: String): SelectedCourseTimetable {
        val document = Ksoup.parse(html)
        val table = document.select("table").firstOrNull { table ->
            table.select("tr").any { row -> row.children().count { it.tagName() == "td" || it.tagName() == "th" } >= 8 }
        } ?: error("未找到已选课程课表，请刷新或重新登录。")
        val occupied = mutableMapOf<Pair<Int, Int>, String>()
        val rows = table.select("tr").mapIndexedNotNull { rowIndex, row ->
            var column = 0
            row.children().filter { it.tagName() == "td" || it.tagName() == "th" }.forEach { cell ->
                while (occupied.containsKey(rowIndex to column)) column++
                val copy = cell.clone()
                copy.select("br").forEach { it.after("\n") }
                val text = copy.wholeText().lines().map(String::trim).filter(String::isNotBlank).joinToString("\n")
                val rowSpan = cell.attr("rowspan").toIntOrNull()?.coerceIn(1, 24) ?: 1
                val colSpan = cell.attr("colspan").toIntOrNull()?.coerceIn(1, 8) ?: 1
                repeat(rowSpan) { r -> repeat(colSpan) { c -> occupied[(rowIndex + r) to (column + c)] = text } }
                column += colSpan
            }
            val label = occupied[rowIndex to 0].orEmpty()
            if (!label.matches(Regex("[\\d\\s,，、—–-]+(?:节)?")) || (1..7).any { !occupied.containsKey(rowIndex to it) }) null
            else SelectedCourseTimetable.Row(label.replace(Regex("[,，、]"), "-").replace("节", "").trim(),
                (1..7).map { occupied[rowIndex to it].orEmpty() })
        }
        return SelectedCourseTimetable(rows)
    }
}
