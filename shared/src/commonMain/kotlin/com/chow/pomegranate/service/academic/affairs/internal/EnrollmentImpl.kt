package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.TimetableParser
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/**
 * 教务系统修读模块实现。
 */
internal class EnrollmentImpl(
    private val httpClient: HttpClient,
) : AcademicAffairs.Enrollment {
    override suspend fun getTimetable(semester: Semester): Timetable {
        val html = httpClient.get("/jsxsd/xskb/xskb_list.do") {
            // 学期，yyyy-yyyy-T
            parameter("xnxq01id", semester)
        }.bodyAsText()

        return TimetableParser.parse(html, semester)
    }
}