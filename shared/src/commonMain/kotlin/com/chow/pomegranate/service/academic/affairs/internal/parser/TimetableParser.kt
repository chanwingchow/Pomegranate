package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

/**
 * 课表解析器。
 */
internal object TimetableParser {
    /** 星期 */
    private val dayOfWeeks = DayOfWeek.entries

    /** 同一格子内多门课的分隔线 */
    private val dividerRegex = Regex("-{10,}")

    /** HTML 标签正则 */
    private val tagRegex = Regex("<[^>]+>")

    /** 换行标签正则 */
    private val brRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

    /** 节次正则，如 `[01-02]节`、`[09-10-11]节` */
    private val sectionRegex = Regex("""\[(\d{2}(?:-\d{2})*)]节""")

    /**
     * 解析 [html] 为 [Timetable]。
     *
     * @param html HTML
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        semester: Semester,
    ): Timetable = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        // #kbtable > tbody
        val body = document.getElementById("kbtable")!!
            .firstElementChild()!!

        val courses = ArrayList<TimetableCourse>(24)
        val rowSize = body.childrenSize() - 1

        // #kbtable > tbody > tr > td > div.kbcontent
        for (rowIndex in 1 until rowSize) {
            val tr = body.child(rowIndex)

            for (columnIndex in 1 until tr.childrenSize()) {
                // child(0) 为节次 th，child(1..7) 为星期 td；kbcontent 固定为 child(3)
                val div = tr.child(columnIndex).child(3)

                if (div.text().isBlank()) continue

                courses.collectCourses(dayOfWeeks[columnIndex - 1], div)
            }
        }

        return@withContext Timetable(
            semester = semester,
            courses = courses,
            note = body.lastElementChild()
                ?.child(1)
                ?.text()
                ?.takeIf { it != "未安排时间课程：" },
        )
    }

    /**
     * 解析单个格子内的课程，追加到当前列表。
     *
     * 一格内可能有多门课，以 `---------------------` 分隔。
     */
    private fun MutableList<TimetableCourse>.collectCourses(
        dayOfWeek: DayOfWeek,
        element: Element,
    ) {
        val html = element.html()
        val segments = if ("-----" in html) {
            html.split(dividerRegex)
        } else {
            listOf(html)
        }
        for (raw in segments) {
            val segment = raw.trim()
            if (segment.isEmpty()) continue
            add(
                TimetableCourse(
                    name = getCourseName(segment),
                    teacher = segment.font("老师"),
                    weeks = segment.font("周次(节次)").orEmpty(),
                    classroom = segment.font("教室"),
                    dayOfWeek = dayOfWeek,
                    sections = sectionRegex.findAll(segment)
                        .flatMap { match -> match.groupValues[1].split('-').map(String::toInt) }
                        .toSet(),
                ),
            )
        }
    }

    /**
     * 按 font title 提取文本。
     */
    private fun String.font(title: String): String? {
        val marker = """title="$title""""
        val start = indexOf(marker)
        if (start < 0) return null
        val open = indexOf('>', start) + 1
        val close = indexOf('<', open)
        if (close < 0) return null
        return substring(open, close).trim().ifEmpty { null }
    }

    /**
     * 提取课程名称。
     *
     * 分割后的 segment 可能以 `<br>` 开头，不能直接用 `substringBefore("<br")`。
     */
    private fun getCourseName(html: String): String {
        val br = html.indexOf("<br", ignoreCase = true)
        val head = if (br < 0) html else html.substring(0, br)
        return head.replace(tagRegex, "").trim().ifEmpty {
            html.replace(brRegex, "\n")
                .replace(tagRegex, "")
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }
    }
}