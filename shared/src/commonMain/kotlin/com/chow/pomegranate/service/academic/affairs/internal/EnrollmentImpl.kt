package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.parser.CampusCalendarParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.CourseTimetableParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.CourseTranscriptParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.ExamScheduleParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.ExemptionParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.LevelTranscriptParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.TeacherParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.TeachersParser
import com.chow.pomegranate.service.academic.affairs.internal.parser.TimetableParser
import com.chow.pomegranate.service.academic.affairs.model.CampusCalendar
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.academic.affairs.model.Exemption
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import com.chow.pomegranate.service.academic.affairs.model.Teacher
import com.chow.pomegranate.service.academic.affairs.model.Teachers
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

        return CourseTimetableParser.parse(html, semester = semester)
    }

    override suspend fun getExamSchedule(semester: Semester): ExamSchedule {
        val html = httpClient.submitForm(
            "/jsxsd/xsks/xsksap_list",
            parameters {
                append("xqlbmc", "")
                append("xnxqid", "$semester")
                // 1: 期初；2: 期中；3: 期末
                append("xqlb", "")
            },
        ).bodyAsText()

        return ExamScheduleParser.parse(
            html,
            userId = userId.value!!,
            semester = semester,
        )
    }

    override suspend fun getCourseTranscript(): CourseTranscript {
        val html = httpClient.get("/jsxsd/kscj/cjcx_list")
            .bodyAsText()

        return CourseTranscriptParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getLevelTranscript(): LevelTranscript {
        val html = httpClient.get("/jsxsd/kscj/djkscj_list")
            .bodyAsText()

        return LevelTranscriptParser.parse(html, userId = userId.value!!)
    }

    override suspend fun getExemption(semester: Semester): Exemption {
        val html = httpClient.submitForm(
            "/jsxsd/kscj/mtsq_list",
            parameters {
                append("xnxqid", "$semester")
            },
        ).bodyAsText()

        return ExemptionParser.parse(
            html,
            userId = userId.value!!,
            semester = semester,
        )
    }

    override suspend fun getCampusCalendar(semester: Semester): CampusCalendar {
        val html = httpClient.submitForm(
            "jsxsd/jxzl/jxzl_query",
            parameters {
                // 学期，yyyy-yyyy-T
                append("xnxq01id", "$semester")
            },
        ).bodyAsText()

        return CampusCalendarParser.parse(html, semester = semester)
    }

    override suspend fun getTeachers(
        query: String,
        pageIndex: Int,
    ): Teachers {
        val html = httpClient.submitForm(
            "/jsxsd/jsxx/jsxx_list",
            parameters {
                // 姓名
                append("jsxm", query)
                // 页码
                append("pageIndex", "$pageIndex")
            },
        ).bodyAsText()

        return TeachersParser.parse(html)
    }

    override suspend fun getTeacher(id: String): Teacher {
        val html = httpClient.get("jsxsd/jsxx/jsxx_query_detail") {
            // 工号
            parameter("jg0101id", id)
        }.bodyAsText()

        return TeacherParser.parse(html, id = id)
    }
}