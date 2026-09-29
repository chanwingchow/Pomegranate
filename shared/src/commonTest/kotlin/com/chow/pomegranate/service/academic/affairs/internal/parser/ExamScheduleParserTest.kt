package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 考试安排解析测试。
 *
 * 覆盖一次真实的上线阻断缺陷：原实现把时间串 split(limit = 2) 后取下标 2，
 * 只要有考试数据就必然 IndexOutOfBoundsException，整个考试模块不可用。
 */
class ExamScheduleParserTest {
    @Test
    fun parsesEveryExamRow() = runBlocking {
        val schedule = ExamScheduleParser.parse(
            html = EXAM_HTML,
            userId = "202400000001",
            semester = Semester(2026, Semester.Term.First),
        )

        assertEquals(2, schedule.exams.size)

        val first = schedule.exams.first()
        assertEquals("CS101", first.courseId)
        assertEquals("程序设计", first.courseName)
        assertEquals(LocalDateTime.parse("2026-01-05T09:00"), first.startTime)
        assertEquals(LocalDateTime.parse("2026-01-05T11:00"), first.endTime)
        assertEquals(Campus.Canton, first.campus)
        assertEquals("实验楼201", first.classroom)

        val second = schedule.exams[1]
        assertEquals(Campus.Foshan, second.campus)
    }

    @Test
    fun malformedRowIsSkippedInsteadOfFailingWholePage() = runBlocking {
        val schedule = ExamScheduleParser.parse(
            html = MALFORMED_HTML,
            userId = "202400000001",
            semester = Semester(2026, Semester.Term.First),
        )

        // 只有第二行时间合法，第一行（没有 ~ 分隔）被跳过，第三行还在
        assertEquals(2, schedule.exams.size)
        assertEquals("数据结构", schedule.exams.first().courseName)
        assertEquals("操作系统", schedule.exams[1].courseName)
    }

    private companion object {
        fun row(
            courseId: String,
            courseName: String,
            time: String,
            campus: String,
            classroom: String,
        ) = """
            <tr>
              <td>1</td>
              <td>$courseId</td>
              <td>$courseName</td>
              <td>$time</td>
              <td>$campus</td>
              <td>$classroom</td>
            </tr>
        """.trimIndent()

        val EXAM_HTML = """
            <html><body>
              <table id="dataList">
                <tbody>
                  <tr><td>序号</td><td>课程编号</td><td>课程名称</td><td>考试时间</td><td>校区</td><td>考场</td></tr>
                  ${row("CS101", "程序设计", "2026-01-05 09:00~2026-01-05 11:00", "广州校区", "实验楼201")}
                  ${row("CS102", "数据结构", "2026-01-07 14:30~2026-01-07 16:30", "佛山校区", "一教305")}
                </tbody>
              </table>
            </body></html>
        """.trimIndent()

        val MALFORMED_HTML = """
            <html><body>
              <table id="dataList">
                <tbody>
                  <tr><td>序号</td><td>课程编号</td><td>课程名称</td><td>考试时间</td><td>校区</td><td>考场</td></tr>
                  ${row("CS101", "待定考试", "时间待定", "广州校区", "实验楼201")}
                  ${row("CS102", "数据结构", "2026-01-07 14:30~2026-01-07 16:30", "佛山校区", "一教305")}
                  ${row("CS103", "操作系统", "2026-01-09 09:00~2026-01-09 11:00", "广州校区", "二教101")}
                </tbody>
              </table>
            </body></html>
        """.trimIndent()
    }
}
