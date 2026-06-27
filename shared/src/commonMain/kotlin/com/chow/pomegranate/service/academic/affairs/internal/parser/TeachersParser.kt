package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.BasicTeacher
import com.chow.pomegranate.service.academic.affairs.model.Teachers
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 教师列表解析器。
 */
internal object TeachersParser {
    private val pageCountRegex = Regex("""共(\d+)页""")

    /**
     * 解析 [html] 为 [Teachers]。
     *
     * @param html HTML
     */
    suspend fun parse(
        html: String,
    ): Teachers = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Class("Nsb_r_list Nsb_table"))!!
            .child(2)

        // 教师项
        val items = mutableListOf<BasicTeacher>()

        val rowLastIndex = tbody.childrenSize() - 1

        // 跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            items += BasicTeacher(
                id = tr.child(1).text(),
                name = tr.child(2).text(),
                department = tr.child(3).text(),
            )
        }

        // 页数信息
        val pageInfoDiv = document.selectFirst(Evaluator.Class("Nsb_r_list_fy"))!!
        // 当前页数
        val pageIndex = pageInfoDiv.child(0)
            .child(1)
            .attr("value")
            .toInt()
        // 总页数
        val pageCount = pageCountRegex.find(pageInfoDiv.child(1).child(0).text())
            ?.groupValues?.get(1)?.toInt() ?: 1

        return@withContext Teachers(
            pageIndex = pageIndex,
            pageCount = pageCount,
            items = items,
        )
    }
}