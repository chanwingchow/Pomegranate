package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.LevelGrade
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * 等级考试成绩解析器。
 */
internal object LevelTranscriptParser {
    /**
     * 解析 [html] 为 [LevelTranscript]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): LevelTranscript = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "dataList"))

        val tbody = document.getElementById("dataList")!!
            .firstElementChild()!!

        // 等级考试成绩项
        val items = mutableListOf<LevelGrade>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #dataList > tbody > tr，跳过表头
        for (rowIndex in 2..rowLastIndex) {
            // #dataList > tbody > tr
            val tr = tbody.child(rowIndex)

            items += LevelGrade(
                name = tr.child(1).text(),
                score = tr.child(4).text(),
                date = LocalDate.parse(tr.child(8).text()),
            )
        }

        return@withContext LevelTranscript(
            userId = userId,
            items = items,
        )
    }
}