package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.AcademicPlan
import com.chow.pomegranate.service.academic.affairs.model.PlannedCourse
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 学业计划解析器。
 */
internal object AcademicPlanParser {
    /**
     * 解析 [html] 为 [AcademicPlan]。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): AcademicPlan = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "dataList"))

        // #dataList > tbody
        val tbody = document.getElementById("dataList")!!
            .firstElementChild()!!

        // 计划课程
        val courses = mutableListOf<PlannedCourse>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #dataList > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            // #dataList > tbody > tr
            val tr = tbody.child(rowIndex)

            courses += PlannedCourse(
                semester = Semester.parse(tr.child(1).text()),
                courseId = tr.child(2).text(),
                name = tr.child(3).text(),
                department = tr.child(4).text(),
                credits = tr.child(5).text().toDouble(),
                hours = tr.child(6).text().toInt(),
                assessment = tr.child(7).text(),
                attribute = tr.child(8).text(),
                isExamAssessed = tr.child(9).text() == "是",
            )
        }

        return@withContext AcademicPlan(
            userId = userId,
            courses = courses,
        )
    }
}