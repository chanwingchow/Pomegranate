package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.CourseDropLogsParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.CourseSystemEntranceParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.CreditSummaryParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.SelectedCoursesParser
import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.chow.pomegranate.service.academic.affairs.model.CourseOperateResult
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchResponse
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectPriority
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import io.ktor.http.parameters
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.isoDayNumber

/**
 * 教务系统选课系统模块实现。
 */
internal class CourseSystemImpl(
    private val httpClient: HttpClient,
    private val userId: StateFlow<String?>,
) : AcademicAffairs.CourseSystem {
    override suspend fun getEntrances(): List<CourseSystemEntrance> {
        val html = httpClient.get("/jsxsd/xsxk/xklc_list").bodyAsText()

        return CourseSystemEntranceParser.parse(html, userId = userId.value!!)
    }

    override suspend fun enterCourseSystem(id: String) {
        httpClient.get("/jsxsd/xsxk/xsxk_index") {
            parameter("jx0502zbid", id)
        }
    }

    override suspend fun getCreditSummary(): CreditSummary {
        val html = httpClient.get("/jsxsd/xsxk/xsxk_tzsm").bodyAsText()

        return CreditSummaryParser.parse(html)
    }

    override suspend fun getSelectedCourses(): List<SelectedCourse> {
        val html = httpClient.get("/jsxsd/xsxkjg/comeXkjglb").bodyAsText()

        return SelectedCoursesParser.parse(html)
    }

    override suspend fun getCourseDropLogs(): List<CourseDropLog> {
        val html = httpClient.get("/jsxsd/xsxkjg/getTkrzList").bodyAsText()

        return CourseDropLogsParser.parse(html)
    }

    override suspend fun searchCourses(params: CourseSearchParam): CourseSearchResponse {
        // 选课课程分类
        val category = when (params) {
            is CourseSearchParam.Basic -> CourseSelectCategory.Basic
            is CourseSearchParam.CrossGrade -> CourseSelectCategory.CrossGrade
            is CourseSearchParam.General -> CourseSelectCategory.General
            is CourseSearchParam.Interprofessional -> CourseSelectCategory.Interprofessional
            is CourseSearchParam.Professional -> CourseSelectCategory.Professional
            is CourseSearchParam.Optional -> CourseSelectCategory.Optional
        }

        val body = httpClient.submitForm(
            "jsxsd/xsxkkc/xsxk${category.searchRoute}xk",
            parameters {
                append("iDisplayStart", "${params.pageSize * params.pageIndex}")
                append("iDisplayLength", "${params.pageSize}")
            },
        ) {
            when (params) {
                is CourseSearchParam.CourseSearchExtraParam -> {
                    parameter("kcxx", params.query.encodeURLParameter())
                    parameter("skls", params.teacher.encodeURLParameter())
                    parameter("skxq", params.dayOfWeek?.isoDayNumber ?: "")
                    parameter("skjc", params.section?.let { "$it-" } ?: "")
                    parameter("sfym", params.filterFull)
                    parameter("sfct", params.filterConflicts)

                    if (params is CourseSearchParam.General) {
                        parameter("xq", params.campus?.run { ordinal + 1 } ?: "")
                    }
                }

                else -> {}
            }
        }.body<CourseSearchResponse>()

        return body
    }

    override suspend fun selectCourse(
        id: String,
        category: CourseSelectCategory,
        priority: CourseSelectPriority?,
    ) {
        val body = httpClient.get("/jsxsd/xsxkkc/${category.operateRoute}xkOper") {
            parameter("jx0404id", id)
            parameter("xkzy", priority?.id ?: "")
            parameter("trjf", "")
            parameter("cxxdlx", "1")
        }.body<CourseOperateResult>()

        check(body.isSuccess) { body.message ?: "" }
    }

    override suspend fun dropCourse(id: String) {
        val body = httpClient.get("/jsxsd/xsxkjg/xstkOper") {
            parameter("jx0404id", id)
        }.body<CourseOperateResult>()

        check(body.isSuccess) { body.message ?: "" }
    }
}