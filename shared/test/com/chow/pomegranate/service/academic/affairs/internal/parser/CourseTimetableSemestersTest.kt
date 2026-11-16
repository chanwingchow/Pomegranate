package com.chow.pomegranate.service.academic.affairs.internal.parser

import com.chow.pomegranate.service.foundation.Semester
import kotlin.test.*

class CourseTimetableSemestersTest {
    @Test fun readsOnlySemesterOptionsAndIgnoresPlaceholdersAndDuplicates() {
        val html = """<select name="other"><option value="2024-2025-1"></option></select>
            <select id="xnxqh" name="xnxqh"><option value="">请选择</option>
            <option value="2025-2026-2">上学期</option><option value="2026-2027-1">本学期</option>
            <option value="2026-2027-1">重复</option><option value="2026-2028-1">无效</option></select>"""
        assertEquals(listOf(Semester.parse("2026-2027-1"), Semester.parse("2025-2026-2")), CourseTimetableParser.parseSemesters(html))
    }
}
