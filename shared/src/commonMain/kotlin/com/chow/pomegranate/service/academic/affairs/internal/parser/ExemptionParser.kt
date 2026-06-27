package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.Exemption
import com.chow.pomegranate.service.academic.affairs.model.ExemptionItem
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * 免听申请解析器。
 */
internal object ExemptionParser {
    /** 内嵌 JavaScript 代码首个字符串参数正则 */
    private val insetJavaScriptUrlRegex = Regex("JsMod\\s*\\(\\s*'([^']+)")

    /**
     * 解析 [html] 为 [Exemption]。
     *
     * @param html HTML
     * @param userId 学号
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        userId: String,
        semester: Semester,
    ): Exemption = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Class("Nsb_r_list Nsb_table"))!!
            .firstElementChild()!!

        // 免听申请项
        val items = mutableListOf<ExemptionItem>()

        val rowLastIndex = tbody.childrenSize() - 1

        // 跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            items += ExemptionItem(
                courseId = tr.child(2).text(),
                courseName = tr.child(3).text(),
                department = tr.child(4).text(),
                teacher = tr.child(5).text(),
                hours = tr.child(6).text().toInt(),
                credits = tr.child(7).text().toDouble(),
                assessment = tr.child(8).text(),
                reason = tr.child(9).text().ifBlank { null },
                status = tr.child(10).text().ifBlank { null },
                appliedDate = tr.child(11).text().ifBlank { null }?.let {
                    LocalDate.parse(it)
                },
                applyUrl = tr.child(12).firstElementChild()?.run {
                    if (hasAttr("onclick")) {
                        insetJavaScriptUrlRegex.find(attr("onclick"))?.groupValues[1]
                    } else {
                        null
                    }
                },
            )
        }

        return@withContext Exemption(
            userId = userId,
            semester = semester,
            items = items,
        )
    }
}