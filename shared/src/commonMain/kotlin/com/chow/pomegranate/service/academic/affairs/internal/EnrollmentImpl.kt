package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.CourseTimetableParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.TimetableParser
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.measureTime

/**
 * 教务系统修读模块实现。
 */
internal class EnrollmentImpl(
    private val httpClient: HttpClient,
    private val userId: StateFlow<String?>,
) : AcademicAffairs.Enrollment {
    override suspend fun getTimetable(semester: Semester): Timetable {
        val html = httpClient.get("/jsxsd/xskb/xskb_list.do") {
            // 学期，yyyy-yyyy-T
            parameter("xnxq01id", semester)
        }.bodyAsText()

        measureTime {
            TimetableParser.parse(
                html,
                userId = userId.value!!,
                semester = semester,
            )
        }.also {
            print(it)
        }

        return TimetableParser.parse(
            html,
            userId = userId.value!!,
            semester = semester,
        )
    }

    override suspend fun getCourseTimetable(semester: Semester): CourseTimetable {
        val html = httpClient.submitForm(
            "/jsxsd/kbcx/kbxx_kc_ifr",
            parameters {
                append("xnxqh", "$semester")
            },
        ) {
            timeout {
                requestTimeoutMillis = 45000
            }
        }.bodyAsText()

        return CourseTimetableParser.parse(html, semester)
    }
}