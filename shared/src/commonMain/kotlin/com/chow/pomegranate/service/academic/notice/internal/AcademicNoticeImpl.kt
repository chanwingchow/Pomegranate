package com.chow.pomegranate.service.academic.notice.internal

import com.chow.pomegranate.service.academic.notice.api.AcademicNotice
import com.chow.pomegranate.service.academic.notice.internal.parser.NoticeParser
import com.chow.pomegranate.service.academic.notice.internal.parser.NoticesParser
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.chow.pomegranate.service.academic.notice.model.NoticePage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request

/**
 * 教务通知实现。
 */
internal class AcademicNoticeImpl(
    private val httpClient: HttpClient,
) : AcademicNotice {
    override suspend fun getNotices(pageIndex: Int): NoticePage {
        val urlString = "/4133/list${pageIndex}.psp"
        val response = httpClient.get(urlString)
        val html = response.bodyAsText()

        val result = NoticesParser.parse(html) {
            response.request.url
        }

        return NoticePage(
            pageIndex = pageIndex,
            notices = result.notices,
            totalPages = result.totalPages,
            hasMorePages = when (val totalPages = result.totalPages) {
                // 服务端给出总页数时以总页数为准，翻过末页也不会继续请求
                null -> result.notices.isNotEmpty()
                else -> pageIndex < totalPages
            },
        )
    }

    override suspend fun getNotice(notice: BasicNotice): Notice {
        val response = httpClient.get(notice.urlString)
        val html = response.bodyAsText()

        return NoticeParser.parse(html, notice = notice) {
            response.request.url
        }
    }
}