package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CourseGrade
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 课程成绩解析器。
 */
internal object CourseTranscriptParser {
    /**
     * 解析 [html] 为 [CourseTranscript]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): CourseTranscript = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        // #dataList > tbody
        val tbody = document.getElementById("dataList")!!
            .firstElementChild()!!

        val items = mutableListOf<CourseGrade>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #dataList > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            items += CourseGrade(
                semester = Semester.parse(tr.child(1).text()),
                courseId = tr.child(2).text(),
                name = tr.child(3).text(),
                regularScore = tr.child(4).text().ifBlank { null },
                labScore = tr.child(5).text().ifBlank { null },
                finalScore = tr.child(6).text().ifBlank { null },
                score = tr.child(7).text(),
                credits = tr.child(8).text().toDouble(),
                hours = tr.child(9).text().toInt(),
                assessment = tr.child(10).text(),
                attribute = tr.child(11).text(),
                category = tr.child(12).text(),
                electiveCategory = tr.child(13).text().ifBlank { null },
                examCategory = tr.child(14).text(),
                mark = tr.child(15).text().ifBlank { null },
                note = tr.child(16).text().ifBlank { null },
            )
        }

        // body > div.Nsb_pw
        val overview = document.body().child(4).run {
            // body > div.Nsb_pw > div.Nsb_r_title
            child(1).remove()
            // #dataList
            lastElementChild()?.remove()
            text().replace("查询条件：全部 ", "")
        }

        return@withContext CourseTranscript(
            userId = userId,
            overview = overview,
            items = items,
        )
    }
}