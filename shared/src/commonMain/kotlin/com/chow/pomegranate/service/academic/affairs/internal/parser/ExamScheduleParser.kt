package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CourseExam
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.format.char

/**
 * 考试安排解析器。
 */
internal object ExamScheduleParser {
    private val dateTimeFormat by lazy {
        // yyyy-MM-dd HH:mm
        Format {
            year(); char('-')
            monthNumber(); char('-')
            day(); char(' ')
            hour(); char(':')
            minute()
        }
    }

    /**
     * 解析 [html] 为 [CourseTimetable]。
     *
     * @param html HTML
     * @param userId 学号
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        userId: String,
        semester: Semester,
    ): ExamSchedule = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "dataList"))

        // #dataList > tbody
        val tbody = document.getElementById("dataList")!!
            .firstElementChild()!!

        // 考试
        val exams = mutableListOf<CourseExam>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #dataList > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            // #dataList > tbody > tr
            val tr = tbody.child(rowIndex)

            val (startTime, endTime) = tr.child(3).text()
                .split('~', limit = 2)
                .map { LocalDateTime.parse(it, dateTimeFormat) }
                .let { it[0] to it[2] }

            val exam = CourseExam(
                courseId = tr.child(1).text(),
                courseName = tr.child(2).text(),
                startTime = startTime,
                endTime = endTime,
                campus = when (val campus = tr.child(4).text()) {
                    "广州校区" -> Campus.Canton
                    "佛山校区" -> Campus.Foshan
                    else -> error("Unknow campus: $campus.")
                },
                classroom = tr.child(5).text(),
            )

            exams.add(exam)
        }

        return@withContext ExamSchedule(
            userId = userId,
            semester = semester,
            exams = exams,
        )
    }
}