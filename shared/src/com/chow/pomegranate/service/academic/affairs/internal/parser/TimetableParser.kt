package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

/**
 * 课表解析器。
 */
internal object TimetableParser {
    /**
     * 解析 [html] 为 [Timetable]。
     *
     * @param html HTML
     * @param userId 学号
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        userId: String,
        semester: Semester,
    ): Timetable = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "kbtable"))

        // #kbtable > tbody
        val tbody = document.getElementById("kbtable")!!
            .firstElementChild()!!

        // 课程
        val courses = ArrayList<TimetableCourse>(24)

        val rowLastIndex = 6
        val columnLastIndex = 7

        // #kbtable > tbody > tr，跳过表头星期
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            // #kbtable > tbody > tr > td，跳过表头节次
            for (columnIndex in 1..columnLastIndex) {
                // #kbtable > tbody > tr > td > div.kbcontent
                val div = tr.child(columnIndex).child(3)

                // 跳过空项
                if (div.childrenSize() == 0) continue

                courses.addAllCourses(div, dayOfWeek = DayOfWeek(columnIndex))
            }
        }

        return@withContext Timetable(
            userId = userId,
            semester = semester,
            courses = courses,
            note = tbody.lastElementChild()!!.child(1).text().takeIf { it != "未安排时间课程：" },
        )
    }

    private fun MutableList<TimetableCourse>.addAllCourses(
        div: Element,
        dayOfWeek: DayOfWeek,
    ) {
        var name: String? = null
        var teacher: String? = null
        var weeks: String? = null
        var classroom: String? = null
        var sections: Set<Int> = emptySet()


        fun reset() {
            name = null
            teacher = null
            weeks = null
            classroom = null
            sections = emptySet()
        }

        fun flush() {
            val courseName = name?.trim().orEmpty()
            // 没课名、也没任何 font 信息 → 跳过（纯 br 残留等）
            if (courseName.isEmpty() && teacher == null && weeks == null && classroom == null) {
                reset()
                return
            }
            add(
                TimetableCourse(
                    name = courseName,
                    teacher = teacher,
                    weeksString = weeks.orEmpty(),
                    classroom = classroom,
                    dayOfWeek = dayOfWeek,
                    sections = sections,
                ),
            )
            reset()
        }

        val nodeSize = div.childNodeSize()

        for (nodeIndex in 0..<nodeSize) {
            when (val node = div.childNode(nodeIndex)) {
                is TextNode -> {
                    val text = node.text().trim()
                    if (text.isEmpty()) continue
                    // 多门课分割线
                    if (text.all { it == '-' }) {
                        flush()
                        continue
                    }
                    // 节次：[03-04]节
                    if (text.startsWith("[") && text.endsWith("]节")) {
                        val raw = text.substring(1, text.length - 2) // 去掉 '[' 和 "]节"
                        sections = raw.split('-').map { it.toInt() }.toSet()
                        continue
                    }
                    // 课程名：段内第一个非空、非节次的 TextNode
                    if (name == null) {
                        name = text
                    }
                }

                is Element -> {
                    if (!node.nameIs("font")) continue
                    val value = node.text().trim().ifEmpty { null }
                    when (node.attr("title")) {
                        "老师" -> teacher = value
                        "周次(节次)" -> weeks = value.orEmpty()
                        "教室" -> classroom = value
                    }
                }
            }
        }

        // 最后一门课
        flush()
    }
}