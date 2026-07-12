package com.chow.pomegranate.service.cet.internal

import com.chow.pomegranate.service.cet.api.CET
import com.chow.pomegranate.service.cet.internal.parser.CetExamParser
import com.chow.pomegranate.service.cet.model.CetExam
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request

/**
 * CET 实现。
 */
internal class CetImpl(
    private val httpClient: HttpClient,
) : CET {
    override suspend fun getExamSchedule(): CetExam {
        val response = httpClient.get("/project/CET/IndexEdu.css")
        val html = response.bodyAsText()

        return CetExamParser.parse(html, response.request.url)
    }
}