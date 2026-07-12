package com.chow.pomegranate.service.cet.internal.parser

import com.chow.pomegranate.service.cet.model.CetExam
import com.chow.pomegranate.service.cet.model.CetExamItem
import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.AmPmMarker
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

/**
 * CET 考试解析器。
 */
object CetExamParser {
    private val dateFormat = LocalDate.Format {
        year()
        char('-')
        monthNumber(padding = Padding.NONE)
        char('-')
        day(padding = Padding.NONE)
    }

    /**
     * 将 [html] 解析为 [CetExam]。
     *
     * @param html HTML
     * @param requestUrl 请求 URL
     */
    suspend fun parse(html: String, requestUrl: Url): CetExam = withContext(Dispatchers.Default) {
        val document = Ksoup.parse(html)

        val divs = document.getElementsByClass("main_info_l")

        val firstDiv = divs.first()!!
        val ul = firstDiv.child(1)
        val spans = ul.child(4).children()

        // 名称
        val name = firstDiv.child(0).text().removeSuffix("：")

        // 年份
        val year = name.substring(0..3).toInt()

        // 笔试日期
        val writtenDate = ul.child(0).text()
            .split("：")[1]
            .let { LocalDate.parse("$year-$it", dateFormat) }

        // 四级笔试语言
        val cet4WrittenLanguages = ul.child(1).text().removePrefix("上午：")
        // 六级笔试语言
        val cet6WrittenLanguages = ul.child(2).text().removePrefix("下午：")

        // 四级口语日期、语言
        val (cet4SpeakingDate, cet4SpeakingLanguages) = spans[0].text()
            .split("：")
            .let { LocalDate.parse("$year-${it[0]}", dateFormat) to it[1] }
        // 六级口语日期、语言
        val (cet6SpeakingDate, cet6SpeakingLanguages) = spans[1].text()
            .split("：")
            .let { LocalDate.parse("$year-${it[0]}", dateFormat) to it[1] }

        return@withContext CetExam(
            name = name,
            cet4Written = CetExamItem(
                date = writtenDate,
                amPmMarker = AmPmMarker.AM,
                languages = cet4WrittenLanguages,
            ),
            cet6Written = CetExamItem(
                date = writtenDate,
                amPmMarker = AmPmMarker.PM,
                languages = cet6WrittenLanguages,
            ),
            cet4Speaking = CetExamItem(
                date = cet4SpeakingDate,
                languages = cet4SpeakingLanguages,
            ),
            cet6Speaking = CetExamItem(
                date = cet6SpeakingDate,
                languages = cet6SpeakingLanguages,
            ),
            note = divs[1].text(),
            pdfUrlString = "${requestUrl.protocol.name}://${requestUrl.host}/project/CET/News/TestDataPlan-CET.pdf",
        )
    }
}