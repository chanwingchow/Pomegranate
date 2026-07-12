package com.chow.pomegranate.service.cet.api

import com.chow.pomegranate.service.cet.internal.CetImpl
import com.chow.pomegranate.service.cet.model.CetExam
import com.chow.pomegranate.service.utils.unsafeSslHttpClient
import io.ktor.client.plugins.defaultRequest

/**
 * CET。
 */
fun CET(): CET {
    val httpClient = unsafeSslHttpClient {
        defaultRequest {
            url("https://resource.neea.edu.cn")
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
}