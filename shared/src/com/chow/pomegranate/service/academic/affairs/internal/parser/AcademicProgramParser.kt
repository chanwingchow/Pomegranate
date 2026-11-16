package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.AcademicProgram
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 培养方案解析器。
 */
internal object AcademicProgramParser {
    /**
     * 解析 [html] 为 [AcademicProgram]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): AcademicProgram = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        return@withContext AcademicProgram(
            userId = userId,
            html = document.selectFirst(Evaluator.Tag("form"))!!.html(),
        )
    }
}
