package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

/**
 * 课程课表解析器。
 */
internal object CourseTimetableParser {
    private const val KBTABLE_MARKER = "id=\"kbtable\""
    private const val TABLE_CLOSE = "</table>"

    /** 换行标签正则（仅 [parseCourseCellFromHtml] 回退路径使用） */
    private val brRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

    /** HTML 标签正则（仅 [parseCourseCellFromHtml] 回退路径使用） */
    private val tagRegex = Regex("<[^>]+>")

    /** 节次表头中的数字对，如 `0102` */
    private val sectionLabelRegex = Regex("\\d{2}")

    /**
     * 解析 [html] 为 [CourseTimetable]。
     *
     * @param html HTML
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        semester: Semester,
    ): CourseTimetable = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html))

        // #kbtable > tbody
        val body = document.getElementById("kbtable")!!
            .firstElementChild()!!

        val slots = parseSlotColumns(body)
        val slotCount = slots.size
        val courses = ArrayList<TimetableCourse>(8192)

        // 行 0、1 为表头，行 2 起为课程行
        for (rowIndex in 2 until body.childrenSize()) {
            val tr = body.child(rowIndex)
            if (tr.childrenSize() < 1 + slotCount) continue

            val name = tr.child(0).text().trim()
            if (name.isEmpty()) continue

            for (slotIndex in 0 until slotCount) {
                val td = tr.child(slotIndex + 1)
                if (td.text().isBlank()) continue

                val divs = td.getElementsByClass("kbcontent1")
                if (divs.isEmpty()) continue

                val slot = slots[slotIndex]
                for (div in divs) {
                    val cellContent = parseCourseCell(div) ?: continue
                    courses += TimetableCourse(
                        name = name,
                        teacher = cellContent.teacher,
                        weeksString = cellContent.weeksString,
                        classroom = cellContent.classroom,
                        dayOfWeek = slot.dayOfWeek,
                        sections = slot.sections,
                        clazz = cellContent.clazz,
                    )
                }
            }
        }

        return@withContext CourseTimetable(
            semester = semester,
            courses = courses,
        )
    }

    /**
     * 截取课表表格。
     */
    private fun extractTableHtml(html: String): String {
        val markerIndex = html.indexOf("id=\"kbtable\"")
        val tableCloseTag = "</table>"

        val tableOpen = html.lastIndexOf("<table", markerIndex)
        val tableClose = html.indexOf(tableCloseTag, markerIndex)

        return html.substring(tableOpen, tableClose + tableCloseTag.length)
    }

    /**
     * 从表头两行解析「星期 × 节次」列布局。
     *
     * 行 0：`th` 带 `colspan` 表示星期；行 1：`td` 从索引 1 起为节次标签（如 `0102`）。
     */
    private fun parseSlotColumns(body: Element): List<SlotColumn> {
        val dayRow = body.child(0)
        val slotRow = body.child(1)
        val slots = ArrayList<SlotColumn>(48)
        var slotTdIndex = 1

        for (dayCell in dayRow.children()) {
            if (!dayCell.tagName().equals("th", ignoreCase = true)) continue

            val dayText = dayCell.text().trim()
            if (dayText.isEmpty()) continue

            val span = dayCell.attr("colspan").toIntOrNull() ?: 1
            val dayOfWeek = parseChineseDay(dayText)

            repeat(span) {
                val label = slotRow.child(slotTdIndex).text().trim()
                slots += SlotColumn(
                    dayOfWeek = dayOfWeek,
                    sections = parseSectionLabel(label),
                )
                slotTdIndex++
            }
        }

        return slots
    }

    /**
     * 解析节次表头标签。
     *
     * `0102` → `{1, 2}`；非四位纯数字时回退为提取所有两位数字。
     */
    private fun parseSectionLabel(label: String): Set<Int> {
        if (label.length == 4) {
            val c0 = label[0]
            val c1 = label[1]
            val c2 = label[2]
            val c3 = label[3]
            if (c0.isDigit() && c1.isDigit() && c2.isDigit() && c3.isDigit()) {
                val first = (c0.code - '0'.code) * 10 + (c1.code - '0'.code)
                val second = (c2.code - '0'.code) * 10 + (c3.code - '0'.code)
                return setOf(first, second)
            }
        }

        return sectionLabelRegex.findAll(label)
            .map { it.value.toInt() }
            .toSet()
    }

    /** 解析中文星期，如 `星期一`。 */
    private fun parseChineseDay(text: String): DayOfWeek = when {
        "一" in text -> DayOfWeek.MONDAY
        "二" in text -> DayOfWeek.TUESDAY
        "三" in text -> DayOfWeek.WEDNESDAY
        "四" in text -> DayOfWeek.THURSDAY
        "五" in text -> DayOfWeek.FRIDAY
        "六" in text -> DayOfWeek.SATURDAY
        "日" in text || "天" in text -> DayOfWeek.SUNDAY
        else -> error("Failed to parse day of week: $text")
    }

    /**
     * 解析格子内 `div.kbcontent1`。
     *
     * 优先 [Element.textNodes]；结构异常时回退到 [Element.html] 分行解析。
     */
    private fun parseCourseCell(div: Element): CourseCellContent? {
        var clazz: String? = null
        var line1: String? = null
        var line2: String? = null
        var line3: String? = null
        var lineCount = 0

        for (node in div.textNodes()) {
            val text = node.text().trim()
            if (text.isEmpty()) continue
            when (lineCount) {
                0 -> clazz = text
                1 -> line1 = text
                2 -> line2 = text
                3 -> line3 = text
                else -> break
            }
            lineCount++
        }

        if (lineCount < 2) return parseCourseCellFromHtml(div)

        return buildCourseCellContent(clazz!!, line1!!, line2, line3)
    }

    private fun parseCourseCellFromHtml(div: Element): CourseCellContent? {
        var clazz: String? = null
        var line1: String? = null
        var line2: String? = null
        var line3: String? = null
        var lineCount = 0

        for (line in div.html()
            .replace(brRegex, "\n")
            .replace(tagRegex, "")
            .lineSequence()
        ) {
            val text = line.trim()
            if (text.isEmpty()) continue
            when (lineCount) {
                0 -> clazz = text
                1 -> line1 = text
                2 -> line2 = text
                3 -> line3 = text
                else -> break
            }
            lineCount++
        }

        if (lineCount < 2) return null

        return buildCourseCellContent(clazz!!, line1!!, line2, line3)
    }

    private fun buildCourseCellContent(
        clazz: String,
        line1: String,
        line2: String?,
        line3: String?,
    ): CourseCellContent {
        val (teacher, weeksString, classroom) = if (line2 != null && line2.startsWith("(")) {
            val pair = parseTeacherWeeks(line1, line2)
            Triple(pair.first, pair.second, line3?.takeIf { it.isNotEmpty() })
        } else {
            val pair = parseTeacherWeeks(line1, null)
            Triple(pair.first, pair.second, line2?.takeIf { it.isNotEmpty() })
        }

        return CourseCellContent(
            clazz = clazz,
            teacher = teacher,
            weeksString = weeksString,
            classroom = classroom,
        )
    }

    /**
     * 从教师行解析教师与周次。
     *
     * 周次在括号内，如 `袁楚芹(1-16周)`；也可能拆成两行 `教师` + `(周次)`。
     */
    private fun parseTeacherWeeks(
        teacherLine: String,
        weeksLine: String?,
    ): Pair<String?, String> {
        val combined = buildString {
            append(teacherLine.trim())
            if (weeksLine != null) append(weeksLine.trim())
        }

        val open = combined.indexOf('(')
        val close = combined.lastIndexOf(')')
        if (open in 1..<close) {
            val teacher = combined.substring(0, open).trim().takeIf { it.isNotEmpty() }
            val weeks = combined.substring(open + 1, close).trim()
            return teacher to weeks
        }

        return teacherLine.trim().takeIf { it.isNotEmpty() } to ""
    }

    /** 表头列：星期 + 节次 */
    private data class SlotColumn(
        val dayOfWeek: DayOfWeek,
        val sections: Set<Int>,
    )

    /** 课表格子内单条排课内容 */
    private data class CourseCellContent(
        val clazz: String,
        val teacher: String?,
        val weeksString: String,
        val classroom: String?,
    )
}
