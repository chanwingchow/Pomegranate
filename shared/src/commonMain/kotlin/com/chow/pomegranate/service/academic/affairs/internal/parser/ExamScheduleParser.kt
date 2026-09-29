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

            // 时间形如 "2026-01-05 09:00~2026-01-05 11:00"。
            // 必须逐行容错：单行时间格式异常只丢这一行，不能让整页考试安排失败
            // （原实现 split(limit = 2) 后取下标 2，只要有考试数据就必然越界崩溃）。
            val timeRange = tr.child(3).text().split('~', limit = 2)
            if (timeRange.size < 2) continue

            val startTime = parseExamTimeOrNull(timeRange[0]) ?: continue
            val endTime = parseExamTimeOrNull(timeRange[1]) ?: continue

            val exam = CourseExam(
                courseId = tr.child(1).text(),
                courseName = tr.child(2).text(),
                startTime = startTime,
                endTime = endTime,
                // 校区文案不认识时按广州校区兜底，不再让整页解析失败
                campus = if (tr.child(4).text().trim() == "佛山校区") {
                    Campus.Foshan
                } else {
                    Campus.Canton
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

    /** 解析考试时间，格式不符时返回 `null` 交由调用方跳过该行。 */
    private fun parseExamTimeOrNull(text: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(text.trim(), dateTimeFormat) }.getOrNull()
}