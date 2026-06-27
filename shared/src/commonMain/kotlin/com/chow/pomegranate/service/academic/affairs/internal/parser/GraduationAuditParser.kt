package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.GraduationAudit
import com.chow.pomegranate.service.academic.affairs.model.GraduationAuditItem
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 毕业审核解析器。
 */
internal object GraduationAuditParser {
    /** 内嵌 JavaScript 代码首个字符串参数正则 */
    private val insetJavaScriptUrlRegex = Regex("\\('([^']*)'\\)")

    /**
     * 解析 [html] 为 [GraduationAudit]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): GraduationAudit = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        // #Form1 > table.Nsb_r_list.Nsb_table > tbody
        // 这里有两个 table，应该选第二个
        val tbody = document.getElementsByClass("Nsb_r_list Nsb_table")[1]
            .firstElementChild()!!

        // 毕业审核项
        val items = mutableListOf<GraduationAuditItem>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #Form1 > table.Nsb_r_list.Nsb_table > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            items += GraduationAuditItem(
                year = tr.child(0).text(),
                batchName = tr.child(1).text(),
                category = tr.child(2).text(),
                enrollmentMethod = tr.child(3).text(),
                degreeGpa = tr.child(4).text().toDouble(),
                completionRate = tr.child(5).text(),
                enrollmentRate = tr.child(6).text(),
                note = tr.child(7).text().ifBlank { null },
                reportUrl = insetJavaScriptUrlRegex.find(tr.child(8).child(0).attr("href"))!!
                    .groupValues[1],
            )
        }

        return@withContext GraduationAudit(
            userId = userId,
            items = items,
        )
    }
}