package com.chow.pomegranate.service.academic.notice.internal.parser

import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.utils.resolveUrl
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 教务通知列表解析器。
 */
internal object NoticesParser {
    /** 列表项选择器。 */
    private const val NOTICE_LIST_CLASS = "news_list list2"

    /** 总页数选择器。 */
    private const val TOTAL_PAGES_CLASS = "all_pages"

    /**
     * 将 [html] 解析为 [NoticeParseResult]。
     *
     * @param html HTML
     * @param lazyRequestUrl 懒加载请求 URL
     */
    suspend fun parse(
        html: String,
        lazyRequestUrl: () -> Url,
    ): NoticeParseResult = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val ul = document.selectFirst(Evaluator.Class(NOTICE_LIST_CLASS))!!

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

        // 分页信息：形如 页码 1/78，用于判断真正的末页，
        // 避免翻过末页后服务端仍返回 200 导致列表一直“正在加载”。
        val totalPages = document.selectFirst(Evaluator.Class(TOTAL_PAGES_CLASS))
            ?.text()
            ?.trim()
            ?.toIntOrNull()

        return@withContext NoticeParseResult(
            notices = notices,
            totalPages = totalPages?.takeIf { it > 0 },
        )
    }
}
