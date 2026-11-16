package com.chow.pomegranate.service.academic.notice.api

import com.chow.pomegranate.service.academic.notice.internal.AcademicNoticeImpl
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.chow.pomegranate.service.academic.notice.model.NoticePage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
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
        // 通知站不在校园网内，放宽一点并显式设超时，避免列表一直转圈
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
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
     *
     * 列表带分页信息，便于判断真正的末页。
     */
    suspend fun getNotices(pageIndex: Int = 1): NoticePage

    /**
     * 返回通知详情。
     */
    suspend fun getNotice(notice: BasicNotice): Notice
}