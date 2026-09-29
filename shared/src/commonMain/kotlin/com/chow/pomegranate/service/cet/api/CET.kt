package com.chow.pomegranate.service.cet.api

import com.chow.pomegranate.service.cet.internal.CetImpl
import com.chow.pomegranate.service.cet.model.CetExam
import com.chow.pomegranate.service.utils.unsafeSslHttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest

/**
 * CET。
 */
fun CET(): CET {
    val httpClient = unsafeSslHttpClient {
        defaultRequest {
            url("https://resource.neea.edu.cn")
        }
        // 这个接口偶发很慢，给足时间；同时设超时，避免 PDF 一直加载不出来
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
    }

    return CetImpl(httpClient)
}

/**
 * CET。
 */
interface CET {
    /**
     * 返回 CET 考试安排。
     */
    suspend fun getExamSchedule(): CetExam

    /**
     * 返回 CET 考试安排 PDF 内容。
     */
    suspend fun getExamSchedulePdf(urlString: String): ByteArray
}