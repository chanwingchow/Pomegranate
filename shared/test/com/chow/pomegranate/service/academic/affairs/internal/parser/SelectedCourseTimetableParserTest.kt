package com.chow.pomegranate.service.academic.affairs.internal.parser

import kotlin.test.*

class SelectedCourseTimetableParserTest {
    @Test fun keepsWeekdayColumnsAndLineBreaks() {
        val data = SelectedCourseTimetableParser.parse("""
            <table><tr><th>节次</th>${(1..7).joinToString("") { "<th>星期$it</th>" }}</tr>
            <tr><td>1,2节</td><td>高等数学<br>林老师<br>教学楼 A301</td>${"<td></td>".repeat(5)}<td>体育</td></tr>
            <tr><td>3-4节</td>${"<td></td>".repeat(7)}</tr></table>
        """.trimIndent())
        assertEquals(2, data.rows.size)
        assertEquals("1-2", data.rows.first().sections)
        assertEquals("高等数学\n林老师\n教学楼 A301", data.rows.first().days[0])
        assertEquals("体育", data.rows.first().days[6])
        assertTrue(data.rows.last().days.all(String::isBlank))
    }

    @Test fun expandsRowSpansWithoutShiftingWeekdays() {
        val data = SelectedCourseTimetableParser.parse("""
            <table><tr><td>1-2节</td><td rowspan="2">连堂课</td>${"<td></td>".repeat(6)}</tr>
            <tr><td>3-4节</td><td>第二天的课</td>${"<td></td>".repeat(5)}</tr></table>
        """)
        assertEquals("连堂课", data.rows[1].days[0])
        assertEquals("第二天的课", data.rows[1].days[1])
    }

    @Test fun rejectsLoginOrErrorHtmlInsteadOfReportingEmptyTimetable() {
        assertFailsWith<IllegalStateException> { SelectedCourseTimetableParser.parse("<html>请重新登录</html>") }
    }
}
