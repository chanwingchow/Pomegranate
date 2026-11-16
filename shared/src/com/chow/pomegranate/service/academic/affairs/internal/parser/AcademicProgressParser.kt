package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.AcademicProgress
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgressCourse
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgressModule
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 学业进度解析器。
 */
internal object AcademicProgressParser {
    /**
     * 解析 [html] 为 [AcademicProgress]。
     *
     * @param html HTML
     * @param userId 学号
     * @param isPrimary 是否为主修
     */
    suspend fun parse(
        html: String,
        userId: String,
        isPrimary: Boolean,
    ): AcademicProgress = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val tables = document.getElementsByClass("Nsb_r_list Nsb_table")

        // 模块进度
        val modules = mutableListOf<AcademicProgressModule>()

        for (table in tables) {
            val tbody = table.firstElementChild()!!

            // 课程进度
            val courses = mutableListOf<AcademicProgressCourse>()

            val rowLastIndex = tbody.childrenSize() - 2

            for (rowIndex in 2..rowLastIndex) {
                val tr = tbody.child(rowIndex)

                courses += AcademicProgressCourse(
                    moduleName = tr.child(0).text(),
                    attribute = tr.child(1).text(),
                    courseId = tr.child(2).text(),
                    name = tr.child(3).text(),
                    credits = tr.child(4).text().toDouble(),
                    suggestedSemesterString = tr.child(5).text(),
                    exemption = tr.child(6).text().ifBlank { null },
                    requiredCredits = tr.child(7).text().toDoubleOrNull(),
                    earnedCredits = tr.child(8).text().toDoubleOrNull(),
                )
            }

            // 模块名称
            val name = tbody.child(0).firstElementChild()!!.textNodes()[0].text().trim()
            // 最后一行包含模块总计学分
            val lastTr = tbody.lastElementChild()!!
            val lastTrChildrenSize = lastTr.childrenSize()

            modules += AcademicProgressModule(
                name = name,
                requiredCredits = lastTr.child(lastTrChildrenSize - 2).text().toDoubleOrNull(),
                earnedCredits = lastTr.child(lastTrChildrenSize - 1).text().toDouble(),
                courses = courses,
            )
        }

        return@withContext AcademicProgress(
            userId = userId,
            isPrimary = isPrimary,
            modules = modules,
        )
    }
}