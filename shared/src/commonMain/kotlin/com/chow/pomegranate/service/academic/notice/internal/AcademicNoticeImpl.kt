package com.chow.pomegranate.service.academic.notice.internal

import com.chow.pomegranate.service.academic.notice.api.AcademicNotice
import com.chow.pomegranate.service.academic.notice.internal.parser.NoticeParser
import com.chow.pomegranate.service.academic.notice.internal.parser.NoticesParser
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
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
    override suspend fun getNotices(pageIndex: Int): List<BasicNotice> {
        val urlString = "/4133/list${pageIndex}.psp"
        val response = httpClient.get(urlString)
        val html = response.bodyAsText()

        return NoticesParser.parse(html) {
            response.request.url
        }
    }

    override suspend fun getNotice(notice: BasicNotice): Notice {
        val response = httpClient.get(notice.urlString)
        val html = response.bodyAsText()

        return NoticeParser.parse(html, notice = notice) {
            response.request.url
        }
    }
}