package com.chow.pomegranate.ui.screen.main.timetable

import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.service.foundation.Campus
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 课表展示模型测试。 */
class TimetableDisplayTest {
    @Test
    fun increasingTheWeekMovesDatesForwardAcrossMonthBoundaries() {
        val today = LocalDate(2026, 11, 29)
        assertEquals(LocalDate(2026, 11, 23), weekDates(0, today).first())
        assertEquals(LocalDate(2026, 11, 30), weekDates(1, today).first())
        assertEquals(LocalDate(2026, 12, 6), weekDates(1, today).last())
        assertEquals(7, weekDates(1, today).size)
    }

    @Test fun contiguousSectionsShowClockTimesIncludingTheLastEveningPeriod() {
        assertEquals("08:00–09:40", timetableTimeFor(listOf(1, 2)))
        assertEquals("08:00–11:40", timetableTimeFor(listOf(1, 2, 3, 4)))
        assertEquals("10:00–10:45", timetableTimeFor(listOf(3)))
        assertEquals("20:30–22:10", timetableTimeFor(listOf(11, 12)))
        assertEquals("20:20–21:50", timetableTimeFor(listOf(11, 12), Campus.Foshan))
        assertEquals("14:10–15:50", timetableTimeFor(listOf(5, 6), Campus.Canton))
        assertEquals("14:00–15:30", timetableTimeFor(listOf(5, 6), Campus.Foshan))
        assertEquals(null, timetableTimeFor(listOf(13)))
        Campus.entries.forEach { assertEquals(12, sectionTimeRanges(it).size) }
        assertEquals(null, timetableTimeFor(listOf(1, 4)))
    }

    @Test fun dailyPagingCrossesSundayWithoutTreatingNextSundayAsToday() {
        val today = LocalDate(2026, 11, 15)
        val index = dayIndexForDate(today, currentWeek = 8, today = today)
        assertEquals(8 * 7 + 6, index)
        assertEquals(today, timetableDateForDay(index, 8, today))
        assertEquals(LocalDate(2026, 11, 16), timetableDateForDay(index + 1, 8, today))
        assertEquals(LocalDate(2026, 11, 14), timetableDateForDay(index - 1, 8, today))
        val nextSunday = timetableDateForDay(index + 7, 8, today)
        assertEquals(LocalDate(2026, 11, 22), nextSunday)
        assertTrue(nextSunday != today)
    }

    @Test fun calendarMonthGridIncludesEveryDayInSevenEqualColumns() {
        val november = calendarMonthCells(LocalDate(2026, 11, 15))
        assertEquals(42, november.size)
        assertEquals(6, november.indexOf(LocalDate(2026, 11, 1)))
        assertEquals(30, november.filterNotNull().size)
        assertEquals(LocalDate(2026, 11, 30), november.filterNotNull().last())
        val leapFebruary = calendarMonthCells(LocalDate(2024, 2, 1)).filterNotNull()
        assertEquals(29, leapFebruary.size)
        assertEquals(LocalDate(2024, 2, 29), leapFebruary.last())
    }

    @Test fun optionalOtherWeeksKeepsInactiveCoursesWithoutChangingTheDefault() {
        val active = course("当前周课程", DayOfWeek.MONDAY, setOf(1, 2), "1-2周")
        val inactive = course("其他周课程", DayOfWeek.MONDAY, setOf(1, 2), "5-8周")
        assertEquals(listOf(active), TimetableDisplay.of(listOf(active, inactive), week = 1).coursesAt(0, DayOfWeek.MONDAY))
        assertEquals(listOf(active, inactive), TimetableDisplay.of(listOf(active, inactive), week = 1, includeOtherWeeks = true).coursesAt(0, DayOfWeek.MONDAY))
        assertTrue(!inactive.isInWeek(1))
    }

    @Test fun calendarUsesConfiguredCurrentWeekAndUtcDatesRoundTrip() {
        val today = LocalDate(2026, 11, 15)
        assertEquals(LocalDate(2026, 11, 9), weekDates(8, today, currentWeek = 8).first())
        assertEquals(LocalDate(2026, 11, 16), weekDates(9, today, currentWeek = 8).first())
        assertEquals(9, weekForDate(LocalDate(2026, 11, 20), currentWeek = 8, today = today))
        val date = LocalDate(2026, 11, 20)
        assertEquals(date, utcDate(date.utcMillis()))
    }

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

    /** 超出支持范围的节次会被跳过。 */
    @Test
    fun skipCoursesWithUnsupportedSections() {
        val display = TimetableDisplay.of(
            listOf(
                course(name = "越界", dayOfWeek = DayOfWeek.MONDAY, sections = setOf(13, 14)),
            ),
        )

        assertTrue(display.courses.isEmpty())
    }

    @Test
    fun keepsSingleSectionsAndCoursesSpanningMultipleRowsIncludingTheEvening() {
        val display = TimetableDisplay.of(listOf(
            course("单节课", DayOfWeek.MONDAY, setOf(3)),
            course("跨四节", DayOfWeek.TUESDAY, setOf(1, 2, 3, 4)),
            course("晚课", DayOfWeek.FRIDAY, setOf(11, 12)),
        ))
        assertEquals("单节课", display.coursesAt(1, DayOfWeek.MONDAY).single().name)
        assertEquals("跨四节", display.coursesAt(0, DayOfWeek.TUESDAY).single().name)
        assertEquals("跨四节", display.coursesAt(1, DayOfWeek.TUESDAY).single().name)
        assertEquals("晚课", display.coursesAt(5, DayOfWeek.FRIDAY).single().name)
        assertEquals(6, TIMETABLE_ROW_COUNT)
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

        assertTrue(TimetableDisplay.of(listOf(course), week = 3).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 4).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isEmpty())
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

        assertTrue(TimetableDisplay.of(listOf(course), week = 1).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 3).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 8).courses.isEmpty())
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

        assertTrue(TimetableDisplay.of(listOf(course), week = 2).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 4).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 8).courses.isNotEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 0).courses.isEmpty())
        assertTrue(TimetableDisplay.of(listOf(course), week = 1).courses.isEmpty())
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
