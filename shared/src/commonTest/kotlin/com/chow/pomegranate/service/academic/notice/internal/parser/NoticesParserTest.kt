package com.chow.pomegranate.service.academic.notice.internal.parser

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoticesParserTest {
    @Test
    fun parsesNoticesAndTotalPages() = runBlocking {
        val result = NoticesParser.parse(html = LIST_HTML, lazyRequestUrl = { Url(BASE_URL) })

        assertEquals(2, result.notices.size)
        assertEquals(78, result.totalPages)
        assertEquals("关于教学安排的通知", result.notices.first().title)
        assertEquals("2026-09-25", result.notices.first().date)
        assertEquals(
            "https://jwc.gdufe.edu.cn/2026/0925/c4133a243499/page.htm",
            result.notices.first().urlString,
        )
    }

    @Test
    fun missingPaginationLeavesTotalPagesNull() = runBlocking {
        val html = LIST_HTML.substringBefore("<div id=\"wp_paging_w6\">")

        val result = NoticesParser.parse(html = html, lazyRequestUrl = { Url(BASE_URL) })

        assertEquals(2, result.notices.size)
        assertNull(result.totalPages)
    }

    private companion object {
        const val BASE_URL = "https://jwc.gdufe.edu.cn/4133/list1.psp"

        /**
         * 取自教务通知列表页的最小结构：列表项 + “页码 1/78”分页块。
         */
        val LIST_HTML = """
            <html><body>
              <ul class="news_list list2">
                <li class="news n1 clearfix">
                  <span class="news_title">
                    <a href='/2026/0925/c4133a243499/page.htm' target='_blank'
                       title='关于教学安排的通知'>关于教学安排的通知</a>
                  </span>
                  <span class="news_meta">2026-09-25</span>
                </li>
                <li class="news n2 clearfix">
                  <span class="news_title">
                    <a href='/2026/0918/c4133a243090/page.htm' target='_blank'
                       title='关于做好遴选工作的通知'>关于做好遴选工作的通知</a>
                  </span>
                  <span class="news_meta">2026-09-23</span>
                </li>
              </ul>
              <div id="wp_paging_w6">
                <ul class="wp_paging clearfix">
                  <li class="page_jump">
                    <span class="pages">页码&nbsp;<em class="curr_page">1</em>/<em class="all_pages">78</em></span>
                  </li>
                </ul>
              </div>
            </body></html>
        """.trimIndent()
    }
}
