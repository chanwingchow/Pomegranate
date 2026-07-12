package com.chow.pomegranate.service.academic.notice.internal.parser

import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.shared.resolveUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 教务通知列表解析器。
 */
internal object NoticesParser {
    suspend fun parse(
        html: String,
        lazyRequestUrl: () -> Url,
    ): List<BasicNotice> = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val ul = document.selectFirst(Evaluator.Class("news_list list2"))!!

        // 通知列表
        val notices = mutableListOf<BasicNotice>()

        val lastIndex = ul.childrenSize() - 1

        for (index in 0..lastIndex) {
            val li = ul.child(index)

            val a = li.child(0).child(0)
            val date = li.child(1).text()

            notices += BasicNotice(
                title = a.attr("title"),
                date = date,
                urlString = resolveUrl(a.attr("href"), lazyRequestUrl = lazyRequestUrl),
            )
        }

        return@withContext notices
    }
}