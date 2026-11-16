package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.CourseOperateResult
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.SelectableCourse
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import org.koin.core.annotation.Single

/** 选课系统仓库。 */
@Single
class CourseSelectionRepository(
    private val academicAffairs: AcademicAffairs,
) {
    /** 进入最新批次并返回选课概览。 */
    suspend fun getOverview(): CourseSelectionOverview {
        val entrance = academicAffairs.courseSystem.getEntrances()
            .latestCourseSystemEntrance()
            ?: error("当前没有可用的选课批次。")
        academicAffairs.courseSystem.enterCourseSystem(entrance.id)
        return CourseSelectionOverview(
            entrance = entrance,
            credits = academicAffairs.courseSystem.getCreditSummary(),
            selectedCourses = academicAffairs.courseSystem.getSelectedCourses(),
        )
    }

    /** 按课程名称或教师搜索通识课。 */
    suspend fun search(query: String): List<SelectableCourse> {
        return academicAffairs.courseSystem.searchCourses(
            CourseSearchParam.General(
                query = query.trim(),
                filterFull = true,
                filterConflicts = true,
                pageSize = 50,
            ),
        ).items
    }

    /** 选择通识课。 */
    suspend fun select(id: String): CourseOperateResult =
        academicAffairs.courseSystem.selectCourse(id, CourseSelectCategory.General)

    /** 退课。 */
    suspend fun drop(id: String): CourseOperateResult = academicAffairs.courseSystem.dropCourse(id)
}

/** 选课页概览数据。 */
data class CourseSelectionOverview(
    val entrance: CourseSystemEntrance,
    val credits: CreditSummary,
    val selectedCourses: List<SelectedCourse>,
)

/** 返回结束时间最晚的选课批次。 */
internal fun List<CourseSystemEntrance>.latestCourseSystemEntrance(): CourseSystemEntrance? =
    maxByOrNull(CourseSystemEntrance::endTime)
