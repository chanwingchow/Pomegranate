package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.format.char

/**
 * 选课系统入口解析器。
 */
object CourseSystemEntranceParser {
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
     * 解析 [html] 为 [List]<[CourseSystemEntrance]>。
     *
     * @param html HTML
     * @param userId 学号
     */
    suspend fun parse(
        html: String,
        userId: String,
    ): List<CourseSystemEntrance> = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "tbKxkc"))

        val tbody = document.getElementById("tbKxkc")!!
            .firstElementChild()!!

        // 选课系统入口
        val entrances = mutableListOf<CourseSystemEntrance>()

        val rowLastIndex = tbody.childrenSize() - 1

        // #tbKxkc > tbody > tr，跳过表头
        for (rowIndex in 1..rowLastIndex) {
            val tr = tbody.child(rowIndex)

            entrances += CourseSystemEntrance(
                id = URLBuilder(tr.child(6).child(0).attr("href")).parameters["jx0502zbid"]!!,
                userId = userId,
                semester = Semester.parse(tr.child(0).text()),
                name = tr.child(1).text(),
                startTime = LocalDateTime.parse(tr.child(2).text(), dateTimeFormat),
                endTime = LocalDateTime.parse(tr.child(3).text(), dateTimeFormat),
            )
        }

        return@withContext entrances
    }
}