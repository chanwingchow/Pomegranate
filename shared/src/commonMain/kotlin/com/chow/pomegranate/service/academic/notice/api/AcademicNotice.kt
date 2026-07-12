package com.chow.pomegranate.service.academic.notice.api

import com.chow.pomegranate.service.academic.notice.internal.AcademicNoticeImpl
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest

/**
 * 教务通知。
 */
fun AcademicNotice(): AcademicNotice {
    val httpClient = HttpClient {
        // 默认请求
        defaultRequest {
            url("https://jwc.gdufe.edu.cn")
        }
    }

    return AcademicNoticeImpl(httpClient)
}

/**
 * 教务通知。
 */
interface AcademicNotice {
    /**
     * 返回通知列表。
     */
    suspend fun getNotices(pageIndex: Int = 1): List<BasicNotice>

    /**
     * 返回通知详情。
     */
    suspend fun getNotice(notice: BasicNotice): Notice
}