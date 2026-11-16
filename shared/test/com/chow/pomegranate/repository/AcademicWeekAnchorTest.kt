package com.chow.pomegranate.repository

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AcademicWeekAnchorTest {
    @Test fun increasesOnMondayInsteadOfSevenDaysAfterTheSettingWasChanged() {
        val anchor = AcademicWeekAnchor(8, LocalDate(2026, 11, 9))
        assertEquals(8, anchor.weekOn(LocalDate(2026, 11, 9)))
        assertEquals(8, anchor.weekOn(LocalDate(2026, 11, 15)))
        assertEquals(9, anchor.weekOn(LocalDate(2026, 11, 16)))
        assertEquals(11, anchor.weekOn(LocalDate(2026, 12, 6)))
    }

    @Test fun crossesTheYearAndClampsToTheSupportedSchoolWeeks() {
        val anchor = AcademicWeekAnchor(1, LocalDate(2026, 12, 28))
        assertEquals(1, anchor.weekOn(LocalDate(2027, 1, 3)))
        assertEquals(2, anchor.weekOn(LocalDate(2027, 1, 4)))
        assertEquals(0, anchor.weekOn(LocalDate(2026, 11, 1)))
        assertEquals(MAX_ACADEMIC_WEEK, anchor.weekOn(LocalDate(2027, 12, 1)))
    }
}
