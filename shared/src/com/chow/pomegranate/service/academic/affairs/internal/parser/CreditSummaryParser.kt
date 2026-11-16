package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 选课学分概览解析器。
 */
internal object CreditSummaryParser {
    /**
     * 解析 [html] 为 [CreditSummary]。
     *
     * @param html HTML
     */
    suspend fun parse(
        html: String,
    ): CreditSummary = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Tag("tbody"))!!

        // table > tbody > tr，类别
        val categoryTr = tbody.child(1)
        // table > tbody > tr，选课学分上限
        val limitTr = tbody.child(2)
        // table > tbody > tr，已选学分
        val selectedTr = tbody.child(3)

        // 选课学分进度
        val progresses = List(categoryTr.childrenSize() - 1) {
            // 跳过第一列表头
            val index = it + 1

            CreditSummary.CreditProgress(
                category = categoryTr.child(index).text(),
                selected = selectedTr.child(index).text(),
                limit = limitTr.child(index).text(),
            )
        }

        return@withContext CreditSummary(
            // body > center > div，最后一个 div 有选课提示信息
            note = document.body().firstElementChild()!!.lastElementChild()!!.text(),
            progress = progresses,
        )
    }
}