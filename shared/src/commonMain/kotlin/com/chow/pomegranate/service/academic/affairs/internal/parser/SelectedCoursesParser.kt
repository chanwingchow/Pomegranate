package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Evaluator
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 已选课程解析器。
 */
internal object SelectedCoursesParser {
    /**
     * 解析 [html] 为 [List]<[SelectedCourse]>。
     *
     * @param html HTML
     */
    suspend fun parse(
        html: String,
    ): List<SelectedCourse> = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tbody = document.selectFirst(Evaluator.Tag("tbody"))!!

        val courses = mutableListOf<SelectedCourse>()

        val rowLastIndex = tbody.childrenSize() - 1

        // table > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            courses += SelectedCourse(
                id = URLBuilder(tr.child(8).child(0).attr("href")).parameters["jx0404id"]!!,
                courseId = tr.child(0).text(),
                name = tr.child(1).text(),
                credits = tr.child(2).text().toDouble(),
                attribute = tr.child(3).text(),
                teacher = tr.child(4).text(),
                timeString = tr.child(5).text().takeIf { it.isNotEmpty() },
                classroom = tr.child(6).text().takeIf { it.isNotEmpty() },
            )
        }

        return@withContext courses
    }
}