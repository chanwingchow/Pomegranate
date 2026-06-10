package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 登录错误消息解析器。
 */
internal object LoginErrorMessageParser {
    suspend fun parse(html: String): String = withContext(Dispatchers.Default) {
        // 解析 HTML
        val document = Ksoup.parse(html)
        // 错误信息，可能为 captchaErrorMsg
        return@withContext document.selectFirst(Evaluator.Tag("font"))?.text() ?: document.title()
    }
}