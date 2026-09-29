package com.chow.pomegranate.ui.screen.main.timetable

import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 课表展示模型测试。 */
class TimetableDisplayTest {
    /** 两节连续的课程按行归位。 */
    @Test
    fun placeCourseIntoRow() {
        val display = TimetableDisplay.of(
            listOf(
                course(name = "高等数学", dayOfWeek = DayOfWeek.MONDAY, sections = setOf(1, 2)),
                course(name = "大学英语", dayOfWeek = DayOfWeek.TUESDAY, sections = setOf(3, 4)),
                course(name = "程序设计", dayOfWeek = DayOfWeek.FRIDAY, sections = setOf(9, 10)),
            ),
        )

        assertEquals("高等数学", display.coursesAt(0, DayOfWeek.MONDAY).single().name)
        assertEquals("大学英语", display.coursesAt(1, DayOfWeek.TUESDAY).single().name)
        assertEquals("程序设计", display.coursesAt(4, DayOfWeek.FRIDAY).single().name)
        assertTrue(display.coursesAt(0, DayOfWeek.TUESDAY).isEmpty())
    }

    /** 同一时间格内的多门课程都会被保留。 */
    @Test
    fun keepMultipleCoursesInSameSlot() {
        val display = TimetableDisplay.of(
            listOf(
                course(name = "课程甲", dayOfWeek = DayOfWeek.WEDNESDAY, sections = setOf(5, 6)),
                course(name = "课程乙", dayOfWeek = DayOfWeek.WEDNESDAY, sections = setOf(5, 6)),
            ),
        )

        assertEquals(2, display.coursesAt(2, DayOfWeek.WEDNESDAY).size)
    }

    /** 节次不是连续两节或超出范围时被跳过。 */
    @Test
    fun skipCoursesWithUnsupportedSections() {
        val display = TimetableDisplay.of(
            listOf(
                course(name = "单节", dayOfWeek = DayOfWeek.MONDAY, sections = setOf(3)),
                course(name = "跨节", dayOfWeek = DayOfWeek.MONDAY, sections = setOf(1, 4)),
                course(name = "越界", dayOfWeek = DayOfWeek.MONDAY, sections = setOf(11, 12)),
            ),
        )

        assertTrue(display.courses.isEmpty())
    }

    /** 构造课程。 */
    private fun course(
        name: String,
        dayOfWeek: DayOfWeek,
        sections: Set<Int>,
        weeksString: String = "1-16周",
    ) = TimetableCourse(
        name = name,
        teacher = null,
        weeksString = weeksString,
        classroom = null,
        dayOfWeek = dayOfWeek,
        sections = sections,
    )

    /** 按周次过滤：不在该周的课程不显示。 */
    @Test
    fun filterByWeek() {
        // 只在第 3-4 周上课
        val course = course(
            name = "短期课程",
            dayOfWeek = DayOfWeek.MONDAY,
            sections = setOf(1, 2),
            weeksString = "3-4周",
        )

        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 3).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 5).courses.isEmpty())
    }

    /** 单周课程只在单周出现。 */
    @Test
    fun filterOddWeeks() {
        val course = course(
            name = "单周课",
            dayOfWeek = DayOfWeek.TUESDAY,
            sections = setOf(1, 2),
            weeksString = "1-16周(单)",
        )

        // 周次 0 对应第 1 教学周（单周）
        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isNotEmpty())
        // 周次 1 对应第 2 教学周（双周）
        assertTrue(TimetableDisplay.of(listOf(course), week = 1).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 3).courses.isEmpty())
    }

    /** 双周课程只在双周出现。 */
    @Test
    fun filterEvenWeeks() {
        val course = course(
            name = "双周课",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            sections = setOf(3, 4),
            weeksString = "2-16周(双)",
        )

        assertTrue(TimetableDisplay.of(listOf(course), week = 1).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 3).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isEmpty())
    }

    /** 周次无法识别时按每周都上处理，避免漏课。 */
    @Test
    fun keepCourseWhenWeeksUnparsable() {
        val course = course(
            name = "未知周次",
            dayOfWeek = DayOfWeek.FRIDAY,
            sections = setOf(1, 2),
            weeksString = "",
        )

        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 9).courses.isNotEmpty())
    }
}
