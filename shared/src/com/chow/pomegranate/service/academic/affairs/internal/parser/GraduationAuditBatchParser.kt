package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.GraduationAuditBatch
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 毕业审核批次解析器。
 */
internal object GraduationAuditBatchParser {
    /**
     * 解析 [html] 为 [List]<[GraduationAuditBatch]>。
     *
     * @param html HTML
     */
    suspend fun parse(
        html: String,
    ): List<GraduationAuditBatch> = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val select = document.getElementById("bybm")!!

        // 毕业审核批次
        val batches = mutableListOf<GraduationAuditBatch>()

        val optionLastIndex = select.childrenSize() - 1

        for (optionIndex in 0..optionLastIndex) {
            val option = select.child(optionIndex)

            batches += GraduationAuditBatch(
                id = option.attr("value"),
                name = option.text(),
            )
        }

        return@withContext batches
    }
}