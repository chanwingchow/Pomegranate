package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.ui.component.*
import kotlin.test.*

class AcademicProgressFilteringTest {
    private val courses = listOf(
        StudyProgressCourse("CS101", "程序设计", "必修", 3.0, 3.0, "1,2", "免修审核通过"),
        StudyProgressCourse("CS102", "数据结构", "必修", 4.0, 2.0, "3", null),
        StudyProgressCourse("CS103", "音乐鉴赏", "选修", 2.0, null, "4", null),
    )
    private val detail = StudyAcademicProgress(5.0, 9.0, courses)

    @Test fun completionDistinguishesPartialAndUnknownCredits() {
        assertEquals(listOf(true, false, null), courses.map { it.completed })
        assertEquals(listOf(courses[0]), detail.filteredCourses("", ProgressCourseFilter.Complete).map { it.course })
        assertEquals(listOf(courses[1]), detail.filteredCourses("", ProgressCourseFilter.Incomplete).map { it.course })
        assertEquals(listOf(courses[2]), detail.filteredCourses("", ProgressCourseFilter.Unknown).map { it.course })
    }

    @Test fun searchUsesNameNumberAndAttributeTogetherWithCompletionFilter() {
        assertEquals(listOf(courses[1]), detail.filteredCourses(" cs102 ", ProgressCourseFilter.All).map { it.course })
        assertEquals(2, detail.filteredCourses("必修", ProgressCourseFilter.All).size)
        assertEquals(listOf(courses[1]), detail.filteredCourses("数据", ProgressCourseFilter.Incomplete).map { it.course })
        assertTrue(detail.filteredCourses("CS101", ProgressCourseFilter.Incomplete).isEmpty())
    }

    @Test fun expandedRowsKeepIdentityWhenFilteredEvenWithMissingOrDuplicateCodes() {
        val duplicate = detail.copy(courses = courses.map { it.copy(id = "") })
        val all = duplicate.filteredCourses("", ProgressCourseFilter.All)
        val subset = duplicate.filteredCourses("数据", ProgressCourseFilter.All)
        assertEquals(all[1].key, subset.single().key)
        assertEquals(3, all.map { it.key }.distinct().size)
    }

    @Test fun overviewDoesNotInventMissingRequirementsAndRemainingCreditsNeverGoNegative() {
        assertNull(StudyAchievement(null, 9.0, "学分").completed)
        assertNull(StudyAchievement(5.0, null, "学分").remaining)
        assertEquals(4.0, StudyAchievement(5.0, 9.0, "学分").remaining)
        assertEquals(0.0, StudyAchievement(12.0, 9.0).remaining)
    }
}
