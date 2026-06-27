package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.AcademicAlertParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.AcademicPlanParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.AcademicProgramParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.AcademicProgressParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.GraduationAuditBatchParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.GraduationAuditParser
import com.chow.pomegranate.service.academic.affairs.model.AcademicAlert
import com.chow.pomegranate.service.academic.affairs.model.AcademicPlan
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgram
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgress
import com.chow.pomegranate.service.academic.affairs.model.GraduationAudit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 教务系统学业模块实现。
 */
internal class AcademicImpl(
    private val httpClient: HttpClient,
    private val userId: MutableStateFlow<String?>,
) : AcademicAffairs.Academic {
    override suspend fun getAcademicPlan(): AcademicPlan {
        val html = httpClient.get("/jsxsd/pyfa/pyfa_query")
            .bodyAsText()

        return AcademicPlanParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getAcademicProgress(isPrimary: Boolean): AcademicProgress {
        val html = httpClient.submitForm(
            "jsxsd/pyfa/xyjdcx",
            parameters {
                // 主修 0 或辅修 1
                append("xdlx", if (isPrimary) "0" else "1")
            },
        ) {
            parameter("type", "cx")
        }.bodyAsText()

        return AcademicProgressParser.parse(
            html,
            userId = userId.value!!,
            isPrimary = isPrimary,
        )
    }

    override suspend fun getAcademicAlert(pageIndex: Int): AcademicAlert {
        val html = httpClient.submitForm(
            "/jsxsd/xsxj/xsyjxx.do",
            parameters {
                append("pageIndex", "$pageIndex")
            },
        ).bodyAsText()

        return AcademicAlertParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getAcademicProgram(): AcademicProgram {
        val html = httpClient.get("/jsxsd/pyfa/pyfazd_query")
            .bodyAsText()

        return AcademicProgramParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getGraduationAudit(isPrimary: Boolean): GraduationAudit {
        val batchHtml = httpClient.get("/jsxsd/bygl/bybm")
            .bodyAsText()

        // 毕业审核批次
        val batches = GraduationAuditBatchParser.parse(batchHtml)
            .sortedByDescending { it.name }

        val batch = batches[if (isPrimary) 1 else 0]

        val html = httpClient.submitForm(
            "/jsxsd/bygl/bybmcz.do",
            parameters {
                append("bybm", batch.id)
            },
        ).bodyAsText()

        return GraduationAuditParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getGraduationAuditReport(urlString: String): ByteArray {
        return httpClient.get(urlString) {
            timeout {
                requestTimeoutMillis = 3000
            }
        }.readRawBytes()
    }
}