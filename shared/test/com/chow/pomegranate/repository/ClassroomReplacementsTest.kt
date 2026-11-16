package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.ui.screen.feature.SchoolTimetableFilters
import com.chow.pomegranate.ui.screen.feature.filterSchoolTimetable
import kotlinx.datetime.DayOfWeek
import kotlin.test.*

class ClassroomReplacementsTest {
    @Test fun confirmedBuiltinAliasesKeepTheBuildingDistinctAndPreserveRawSchedules() {
        val rules = ClassroomReplacements(gdufeClassroomAliases)
        assertEquals("北五701", rules.format("乐学楼701"))
        assertEquals("北三503", rules.format("尚学楼503"))
        assertEquals("北二216", rules.format("立德楼216"))
        assertEquals("一教409", rules.format("博学楼409"))
        assertEquals("一教108", rules.format("励学楼108"))
        assertEquals("二教201", rules.format("笃行楼201"))
        assertEquals("实验楼111", rules.format("拓新楼111"))
        assertEquals("北五701", rules.format("乐学楼（北5）701"))
        assertEquals("北三503", rules.format("尚学楼(北三)503"))
        assertTrue(rules.matches("乐学楼701", "北5701"))
        assertTrue(rules.matches("北3503", "北三503"))
        assertEquals("乐学楼课程简介\n林老师\n北五701\n1-16周", rules.formatSchedule("乐学楼课程简介\n林老师\n乐学楼701\n1-16周"))
    }
    @Test fun replacesBuildingTextLiterallyAndKeepsRoomNumbersAndUnmatchedLocations() {
        val rules = ClassroomReplacements(listOf(ClassroomReplacementRule("c", "教学楼 C", "笃行楼 C")))
        assertEquals("笃行楼 C221", rules.format("教学楼 C221"))
        assertEquals("实验楼 A301", rules.format("实验楼 A301"))
        assertEquals("笃行楼 C221 / 笃行楼 C305", rules.format("教学楼 C221 / 教学楼 C305"))
        assertEquals("", rules.format(""))
        val literal = ClassroomReplacements(listOf(ClassroomReplacementRule("literal", "A(1).", "B")))
        assertEquals("B301", literal.format("A(1).301"))
        assertEquals("A11x301", literal.format("A11x301"))
    }

    @Test fun longestMatchWinsAndReplacementOutputIsNotProcessedAgain() {
        val rules = ClassroomReplacements(listOf(
            ClassroomReplacementRule("broad", "教学楼", "新教学楼"),
            ClassroomReplacementRule("specific", "教学楼 C", "笃行楼 C"),
            ClassroomReplacementRule("chained", "笃行楼", "错误结果"),
            ClassroomReplacementRule("disabled", "实验楼", "不应显示", enabled = false),
        ))
        assertEquals("笃行楼 C221", rules.format("教学楼 C221"))
        assertEquals("新教学楼 A201", rules.format("教学楼 A201"))
        assertEquals("实验楼 A301", rules.format("实验楼 A301"))
    }

    @Test fun schoolTimetableCanSearchBothRawAndDisplayedClassroomsWithoutChangingData() {
        val original = TimetableCourse("测试课程", "林老师", "1-16周", "教学楼 C221", DayOfWeek.MONDAY, setOf(1, 2))
        val rules = ClassroomReplacements(listOf(ClassroomReplacementRule("c", "教学楼 C", "笃行楼 C")))
        for (query in listOf("教学楼 C", "笃行楼 C", "C221")) {
            assertEquals(listOf(original), listOf(original).filterSchoolTimetable(query, SchoolTimetableFilters(), rules))
            assertEquals(listOf(original), listOf(original).filterSchoolTimetable("", SchoolTimetableFilters(classroom = query), rules))
        }
        assertEquals("教学楼 C221", original.classroom)
        assertTrue(listOf(original).filterSchoolTimetable("笃行楼 C", SchoolTimetableFilters(teacher = "不存在的教师"), rules).isEmpty())
    }
}
