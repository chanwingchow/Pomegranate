package com.chow.pomegranate.service.academic.notice.internal.parser

import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NoticeParserTest {
    @Test
    fun resolvesLatestNoticeAttachment() = runBlocking {
        // 2026-10-22 通知中的真实附件路径；此前会直接传给 Android 的 UriHandler。
        val path = "/_upload/article/files/1c/3d/d8d231704ea8893420e5c7d804d3/9a12f7fc-48db-4170-b646-7498f010ca42.xls"
        assertEquals("https://jwc.gdufe.edu.cn$path", parseLink(path))
    }

    @Test
    fun resolvesDocumentRelativeAndProtocolRelativeLinks() = runBlocking {
        assertEquals("https://jwc.gdufe.edu.cn/2026/1022/c4133a244445/files/form.xls", parseLink("files/form.xls"))
        assertEquals("https://jwc.gdufe.edu.cn/2026/1022/form.xls", parseLink("../form.xls"))
        assertEquals("https://files.gdufe.edu.cn/form.xls", parseLink("//files.gdufe.edu.cn/form.xls"))
    }

    @Test
    fun preservesAbsoluteAndEmailLinks() = runBlocking {
        assertEquals("https://files.gdufe.edu.cn/form.xls", parseLink("https://files.gdufe.edu.cn/form.xls"))
        assertEquals("mailto:teacher@gdufe.edu.cn", parseLink("mailto:teacher@gdufe.edu.cn"))
    }

    @Test
    fun preservesNoticeMetadataAndBody() = runBlocking {
        val detail = assertIs<Notice.Html>(parse("<p>通知正文</p>"))
        assertEquals(NOTICE.title, detail.title)
        assertEquals(NOTICE.date, detail.date)
        assertEquals(NOTICE.urlString, detail.urlString)
        assertEquals("通知正文", Ksoup.parse(detail.html).text())
    }

    @Test
    fun preservesPdfNoticeDownload() = runBlocking {
        val detail = assertIs<Notice.Document>(
            parse("""<div class="wp_pdf_player" pdfsrc="/_upload/notice.pdf"></div>"""),
        )
        assertEquals("https://jwc.gdufe.edu.cn/_upload/notice.pdf", detail.pdfUrlString)
    }

    private suspend fun parseLink(href: String): String {
        val detail = assertIs<Notice.Html>(parse("""<a href="$href">附件</a>"""))
        return Ksoup.parse(detail.html).selectFirst("a")!!.attr("href")
    }

    private suspend fun parse(body: String): Notice = NoticeParser.parse(
        html = """<div id="d-container">$body</div>""",
        notice = NOTICE,
        lazyRequestUrl = { Url(NOTICE.urlString) },
    )

    private companion object {
        val NOTICE = BasicNotice(
            title = "关于广东省高校健康教育科普课程建设项目拟推荐结果的公示",
            date = "2026-10-22",
            urlString = "https://jwc.gdufe.edu.cn/2026/1022/c4133a244445/page.htm",
        )
    }
}
