package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.AcademicFeature
import com.chow.pomegranate.repository.AcademicFeatureItem
import com.chow.pomegranate.repository.FeatureDataModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcademicFeatureFilteringTest {
    private val items = listOf(
        AcademicFeatureItem("1", "数据结构", "李老师 · 计算机学院", listOf("课程编号" to "CS201")),
        AcademicFeatureItem("2", "大学英语", "王老师 · 外国语学院", listOf("课程编号" to "EN101")),
    )

    @Test
    fun keywordMatchesTitleSupportingTextAndDetailsIgnoringCase() {
        assertEquals(listOf("1"), items.filterByKeyword("cs201").map { it.id })
        assertEquals(listOf("2"), items.filterByKeyword("外国语").map { it.id })
        assertEquals(listOf("1"), items.filterByKeyword("数据").map { it.id })
    }

    @Test
    fun blankKeywordKeepsAllItemsAndOnlyLargeListsAreSearchable() {
        assertEquals(items, items.filterByKeyword("  "))
        assertTrue(AcademicFeature.AcademicPlan.supportsLocalSearch)
        assertTrue(AcademicFeature.CourseTimetable.supportsLocalSearch)
        assertFalse(AcademicFeature.Extracurricular.supportsLocalSearch)
        assertFalse(AcademicFeature.AcademicProgress.supportsLocalSearch)
    }

    /**
     * 教师列表在枚举里就声明为分页功能。
     *
     * 以前它在本地过滤，于是每次进页面都要把全部分页拉回来，再用页数上限兜底，
     * 既放大请求又会静默截断数据。
     */
    @Test
    fun teachersAreDeclaredAsPagedInsteadOfLocallyFiltered() {
        assertEquals(FeatureDataModel.Paged, AcademicFeature.Teachers.dataModel)
        assertFalse(AcademicFeature.Teachers.supportsLocalSearch)
        assertTrue(AcademicFeature.Teachers.isSearchable)

        assertEquals(FeatureDataModel.Whole, AcademicFeature.AcademicPlan.dataModel)
        assertEquals(FeatureDataModel.Whole, AcademicFeature.Extracurricular.dataModel)
    }
}
