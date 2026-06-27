package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.Teacher
import com.chow.pomegranate.service.academic.affairs.model.TeacherCourse
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 教师解析器。
 */
internal object TeacherParser {
    /**
     * 解析 [html] 为 [Teacher]。
     *
     * @param html HTML
     * @param id 工号
     */
    suspend fun parse(
        html: String,
        id: String,
    ): Teacher = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val trs = document.selectFirst(Evaluator.Class("no_border_table"))!!
            .firstElementChild()!!
            .children()

        val hasContact = trs[7].child(1).text() != "联系方式："

        return@withContext Teacher(
            id = id,
            name = trs[1].child(2).text(),
            gender = trs[1].child(4).text().ifBlank { null }
                ?.takeIf { it != "未说明的性别" },
            politics = trs[2].child(1).text().ifBlank { null },
            nation = trs[2].child(3).text().ifBlank { null },
            duty = trs[3].child(1).text().ifBlank { null },
            title = trs[3].child(3).text().ifBlank { null },
            staffCategory = trs[4].child(1).text().ifBlank { null },
            department = trs[4].child(3).text().ifBlank { null },
            office = trs[5].child(1).text().ifBlank { null }
                ?.takeIf { it != "无" },
            qualification = trs[5].child(3).text().ifBlank { null },
            degree = trs[6].child(2).text().ifBlank { null },
            researchField = trs[6].child(4).text().ifBlank { null },
            phoneNumber = if (hasContact) trs[7].child(2).text().ifBlank { null } else null,
            qq = if (hasContact) trs[7].child(4).text().ifBlank { null } else null,
            weChat = if (hasContact) trs[8].child(2).text().ifBlank { null } else null,
            email = if (hasContact) trs[8].child(4).text().ifBlank { null } else null,
            biography = trs[trs.lastIndex - 8].text().ifBlank { null }
                ?.takeIf { it != "暂无数据" },
            recentCourses = parseTeacherCourses(trs, trs.lastIndex - 6),
            plannedCourses = parseTeacherCourses(trs, trs.lastIndex - 4),
            philosophy = trs[trs.lastIndex - 2].text().ifBlank { null }
                ?.takeIf { it != "暂无数据" },
            slogan = trs[trs.lastIndex].text().ifBlank { null }
                ?.takeIf { it != "暂无数据" },
        )
    }

    private fun parseTeacherCourses(trs: Elements, rowIndex: Int): List<TeacherCourse> {
        val tds = trs[rowIndex].selectFirst(Evaluator.Tag("tbody"))!!
            .getElementsByTag("td")
        // 只有 1 个 td → 「查询不到任何相关数据」/ 空表
        if (tds.size == 1) return emptyList()
        return buildList {
            for (i in tds.indices step 4) {
                add(
                    TeacherCourse(
                        name = tds[i + 1].text(),
                        category = tds[i + 2].text(),
                        semester = Semester.parse(tds[i + 3].text()),
                    ),
                )
            }
        }
    }
}