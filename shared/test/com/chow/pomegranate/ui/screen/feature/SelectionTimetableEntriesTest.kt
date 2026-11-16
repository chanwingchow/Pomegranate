package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import kotlin.test.*

class SelectionTimetableEntriesTest {
    private fun course(id: String, name: String) = SelectedCourse(id, id, name, 2.0, "选修", "林老师", "1-16周 星期一 1-2节", "A301")

    @Test fun keepsEveryCourseInTheSameSlotWithoutRequiringBlankLinesFromTheServer() {
        val math = course("math", "高等数学")
        val physics = course("physics", "大学物理")
        val entries = selectionTimetableEntries("高等数学\n林老师\n1-17周(单)\n大学物理\n陈老师\n2-18周(双)", listOf(math, physics))
        assertEquals(listOf(math, physics), entries.map { it.course })
        assertTrue(entries.first().text.contains("1-17周(单)"))
        assertTrue(entries.last().text.contains("2-18周(双)"))
    }

    @Test fun preservesUnknownSchedulesAndNeverAssignsAmbiguousCourseActions() {
        val first = course("first", "大学英语")
        val second = course("second", "大学英语")
        val entries = selectionTimetableEntries("大学英语\n教师甲\nA201\n\n其他课程\nB202", listOf(first, second))
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.course == null })
        assertEquals("大学英语", entries.first().title)
        assertEquals("其他课程\nB202", entries.last().text)
        assertTrue(selectionTimetableEntries("  \n", emptyList()).isEmpty())
    }

    @Test fun doesNotMistakeCoursesWhoseNamesShareAPrefix() {
        val short = course("short", "程序设计")
        val long = course("long", "程序设计实践")
        val entry = selectionTimetableEntries("程序设计实践（2-18周）\nA301", listOf(short, long)).single()
        assertEquals(long, entry.course)
        assertEquals("程序设计实践", entry.title)
    }
}
