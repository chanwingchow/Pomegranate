package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.AcademicAlert
import com.chow.pomegranate.service.academic.affairs.model.AcademicAlertItem
import com.chow.pomegranate.service.academic.affairs.model.AcademicAlertLevel
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 学业预警解析器。
 */
internal object AcademicAlertParser {
    private val pageCountRegex = Regex("""共(\d+)页""")

    /**
     * 解析 [html] 为 [AcademicAlert]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): AcademicAlert = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Class("Nsb_r_list Nsb_table"))!!
            .firstElementChild()!!

        // 预警项
        val items = mutableListOf<AcademicAlertItem>()

        val rowLastIndex = tbody.childrenSize() - 1

        // 跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            items += AcademicAlertItem(
                semester = Semester.parse(tr.child(1).text()),
                level = parseAcademicAlertLevel(tr.child(4).text()),
                description = tr.child(6).text(),
                actualValue = tr.child(7).text(),
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

        return@withContext AcademicAlert(
            userId = userId,
            pageIndex = pageIndex,
            pageCount = pageCount,
            items = items,
        )
    }

    /**
     * 解析学业预警等级。
     */
    private fun parseAcademicAlertLevel(level: String): AcademicAlertLevel {
        return when (level) {
            "红色" -> AcademicAlertLevel.High
            "黄色" -> AcademicAlertLevel.Medium
            "蓝色" -> AcademicAlertLevel.Low
            else -> error("Unknow level: $level")
        }
    }
}