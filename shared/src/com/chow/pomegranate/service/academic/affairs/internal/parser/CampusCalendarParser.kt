package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.academic.affairs.model.CampusCalendar
import com.chow.pomegranate.service.foundation.Semester
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

/**
 * 校历解析器。
 */
internal object CampusCalendarParser {
    private val titleDateFormat = LocalDate.Format {
        year()
        char('年')
        monthNumber()
        char('月')
        day()
        optional { char('日') }
    }

    /**
     * 解析 [html] 为 [CampusCalendar]。
     *
     * @param html HTML
     * @param semester 学期
     */
    suspend fun parse(
        html: String,
        semester: Semester,
    ): CampusCalendar = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(extractTableHtml(html, id = "kbtable"))

        val tbody = document.getElementById("kbtable")!!
            .firstElementChild()!!

        val firstTr = tbody.child(1)
        val lastTr = tbody.child(tbody.childrenSize() - 2)

        val start = LocalDate.parse(
            firstTr.first { it.hasAttr("title") }.attr("title"),
            format = titleDateFormat,
        )
        val end = LocalDate.parse(
            lastTr.last { it.hasAttr("title") }.attr("title"),
            format = titleDateFormat,
        )

        return@withContext CampusCalendar(
            semester = semester,
            duration = start..end,
        )
    }
}