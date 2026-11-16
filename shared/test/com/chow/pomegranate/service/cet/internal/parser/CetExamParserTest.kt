package com.chow.pomegranate.service.cet.internal.parser

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CetExamParserTest {
    @Test
    fun preservesNoteHeadingAndSeparateRegistrationItems() = runBlocking {
        val exam = parse("""<h4>2026年下半年考试报名提示：</h4><ul><li>1．请核对学籍信息。</li><li>2．请及时完成缴费。</li></ul>""")
        assertEquals("2026年下半年考试报名提示：\n\n1．请核对学籍信息。\n\n2．请及时完成缴费。", exam.note)
        assertEquals("英语四级", exam.cet4Written.languages)
        assertEquals("2026-12-12", exam.cet4Written.date.toString())
    }

    @Test
    fun preservesExplicitLineBreaksWithoutDuplicatingNestedParagraphs() = runBlocking {
        val exam = parse("""<ul><li><p>核对<b>学籍</b>信息。<br>信息有误请联系学校。</p></li><li>完成缴费。</li></ul>""")
        assertEquals("核对学籍信息。\n信息有误请联系学校。\n\n完成缴费。", exam.note)
    }

    private suspend fun parse(note: String) = CetExamParser.parse(
        """<div class="main_info_l"><h4>2026年下半年考试时间：</h4><ul>
            <li>笔试：12-12</li><li>上午：英语四级</li><li>下午：英语六级</li><li>口语：</li>
            <li><span>11-21：英语四级</span><span>11-22：英语六级</span></li>
        </ul></div><div class="main_info_l">$note</div>""",
        Url("https://resource.neea.edu.cn/project/CET/IndexEdu.css"),
    )
}
