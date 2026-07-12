package com.chow.pomegranate.service.academic.notice.internal.parser

import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.chow.pomegranate.service.shared.resolveUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 教务通知解析器。
 */
internal object NoticeParser {
    suspend fun parse(
        html: String,
        notice: BasicNotice,
        lazyRequestUrl: () -> Url,
    ): Notice = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val div = document.getElementById("d-container")!!

        // 这里其实是一个 div，有一个 pdfsrc 属性包含 PDF 链接
        val iframe = div.selectFirst(Evaluator.Class("wp_pdf_player"))

        if (iframe == null) {
            return@withContext Notice.Html(
                title = notice.title,
                date = notice.date,
                urlString = notice.urlString,
                html = div.html(),
            )
        } else {
            return@withContext Notice.Document(
                title = notice.title,
                date = notice.date,
                urlString = notice.urlString,
                pdfUrlString = resolveUrl(iframe.attr("pdfsrc"), lazyRequestUrl = lazyRequestUrl),
            )
        }
    }
}