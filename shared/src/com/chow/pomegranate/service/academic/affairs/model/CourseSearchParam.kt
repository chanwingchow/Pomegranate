package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Campus
import kotlinx.datetime.DayOfWeek

/**
 * 选课课程搜索参数。
 */
sealed interface CourseSearchParam {
    /** 页面下标 */
    val pageIndex: Int

    /** 页面内容数量 */
    val pageSize: Int

    /**
     * 选课课程搜索扩展参数。
     */
    sealed interface CourseSearchExtraParam : CourseSearchParam {
        /** 查询，接受课程名称、教师 */
        val query: String

        /** 教师 */
        val teacher: String

        /** 星期 */
        val dayOfWeek: DayOfWeek?

        /** 节次 */
        val section: Section?

        /** 过滤已满 */
        val filterFull: Boolean

        /** 过滤冲突 */
        val filterConflicts: Boolean
    }

    /**
     * 学科基础、专业必修课。
     */
    data class Basic(
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
    ) : CourseSearchParam

    /**
     * 选修课。
     */
    data class Optional(
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
    ) : CourseSearchParam

    /**
     * 通识课。
     */
    data class General(
        override val query: String = "",
        override val teacher: String = "",
        override val dayOfWeek: DayOfWeek? = null,
        override val section: Section? = null,
        override val filterFull: Boolean = false,
        override val filterConflicts: Boolean = false,
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
        val campus: Campus? = null,
    ) : CourseSearchExtraParam

    /**
     * 专业内计划课。
     */
    data class Professional(
        override val query: String = "",
        override val teacher: String = "",
        override val dayOfWeek: DayOfWeek? = null,
        override val section: Section? = null,
        override val filterFull: Boolean = false,
        override val filterConflicts: Boolean = false,
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
    ) : CourseSearchExtraParam

    /**
     * 跨年级。
     */
    data class CrossGrade(
        override val query: String = "",
        override val teacher: String = "",
        override val dayOfWeek: DayOfWeek? = null,
        override val section: Section? = null,
        override val filterFull: Boolean = false,
        override val filterConflicts: Boolean = false,
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
    ) : CourseSearchExtraParam

    /**
     * 跨专业。
     */
    data class Interprofessional(
        override val query: String = "",
        override val teacher: String = "",
        override val dayOfWeek: DayOfWeek? = null,
        override val section: Section? = null,
        override val filterFull: Boolean = false,
        override val filterConflicts: Boolean = false,
        override val pageIndex: Int = DEFAULT_PAGE_INDEX,
        override val pageSize: Int = DEFAULT_PAGE_SIZE,
    ) : CourseSearchExtraParam

    companion object {
        /** 默认页面下标 */
        private const val DEFAULT_PAGE_INDEX = 0

        /** 默认页面内容数量 */
        private const val DEFAULT_PAGE_SIZE = 15
    }
}