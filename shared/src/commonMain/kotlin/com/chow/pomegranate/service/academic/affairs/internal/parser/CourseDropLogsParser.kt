package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 退课记录解析器。
 */
internal object CourseDropLogsParser {
    /**
     * 解析 [html] 为 [List]<[CourseDropLog]>。
     *
     * @param html HTML
     */
    suspend fun parse(
        html: String,
    ): List<CourseDropLog> = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Tag("tbody"))!!

        val logs = mutableListOf<CourseDropLog>()

        val rowLastIndex = tbody.childrenSize() - 1

        // table > tbody > tr
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            logs += CourseDropLog(
                courseId = tr.child(0).text(),
                courseName = tr.child(1).text(),
                credits = tr.child(2).text().toDouble(),
                courseAttribute = tr.child(3).text(),
                teacher = tr.child(4).text(),
                schedule = tr.child(5).run {
                    if (text().isBlank()) {
                        emptyList()
                    } else {
                        textNodes().map { it.text() }
                    }
                },
                selectionCategory = tr.child(6).text(),
                dropType = tr.child(7).text(),
                dropTime = tr.child(8).text(),
                operator = tr.child(9).text(),
                description = tr.child(10).text(),
            )
        }

        return@withContext logs
    }
}