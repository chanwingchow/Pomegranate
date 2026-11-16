package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.CourseOperateResult
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectPriority
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.SelectableCourse
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import org.koin.core.annotation.Single
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.academic.affairs.model.Section
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchResponse

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

    val operationNotice: StateFlow<Boolean?>
        field = MutableStateFlow<Boolean?>(null)
    fun notifyOperation(drop: Boolean) { operationNotice.value = drop }
    fun consumeOperationNotice() { operationNotice.value = null }

    suspend fun getDropLogs(): List<CourseDropLog> = academicAffairs.courseSystem.getCourseDropLogs()
    suspend fun getTimetable() = academicAffairs.courseSystem.getSelectedCourseTimetable()

    suspend fun search(query: String): List<SelectableCourse> = searchPage(query).items

    suspend fun searchPage(query: String, category: CourseSelectCategory = CourseSelectCategory.General,
        page: Int = 0, filterFull: Boolean = true, filterConflicts: Boolean = true,
        filters: CourseSearchFilters = CourseSearchFilters(filterFull = filterFull, filterConflicts = filterConflicts)): CourseSearchResponse {
        val params = when (category) {
            CourseSelectCategory.Basic -> CourseSearchParam.Basic(pageIndex = page, pageSize = 20)
            CourseSelectCategory.Optional -> CourseSearchParam.Optional(pageIndex = page, pageSize = 20)
            CourseSelectCategory.General -> CourseSearchParam.General(campus = filters.campus, query = query.trim(), teacher = filters.teacher.trim(), dayOfWeek = filters.weekday?.let { DayOfWeek.entries.getOrNull(it - 1) }, section = filters.section, filterFull = filters.filterFull, filterConflicts = filters.filterConflicts, pageIndex = page, pageSize = 20)
            CourseSelectCategory.Professional -> CourseSearchParam.Professional(query = query.trim(), teacher = filters.teacher.trim(), dayOfWeek = filters.weekday?.let { DayOfWeek.entries.getOrNull(it - 1) }, section = filters.section, filterFull = filters.filterFull, filterConflicts = filters.filterConflicts, pageIndex = page, pageSize = 20)
            CourseSelectCategory.CrossGrade -> CourseSearchParam.CrossGrade(query = query.trim(), teacher = filters.teacher.trim(), dayOfWeek = filters.weekday?.let { DayOfWeek.entries.getOrNull(it - 1) }, section = filters.section, filterFull = filters.filterFull, filterConflicts = filters.filterConflicts, pageIndex = page, pageSize = 20)
            CourseSelectCategory.Interprofessional -> CourseSearchParam.Interprofessional(query = query.trim(), teacher = filters.teacher.trim(), dayOfWeek = filters.weekday?.let { DayOfWeek.entries.getOrNull(it - 1) }, section = filters.section, filterFull = filters.filterFull, filterConflicts = filters.filterConflicts, pageIndex = page, pageSize = 20)
        }
        return academicAffairs.courseSystem.searchCourses(params)
    }

    suspend fun select(id: String, category: CourseSelectCategory = CourseSelectCategory.General, priority: CourseSelectPriority? = null): CourseOperateResult =
        academicAffairs.courseSystem.selectCourse(id, category, priority)

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

/** 筛选草稿在弹层内编辑，应用后才交给页面 ViewModel 发起请求。 */
@Serializable
data class CourseSearchFilters(
    val teacher: String = "",
    val weekday: Int? = null,
    val section: Section? = null,
    val campus: Campus? = null,
    val filterFull: Boolean = true,
    val filterConflicts: Boolean = true,
) {
    fun forCategory(category: CourseSelectCategory): CourseSearchFilters = when (category) {
        CourseSelectCategory.Basic, CourseSelectCategory.Optional -> CourseSearchFilters()
        CourseSelectCategory.General -> copy(teacher = teacher.trim())
        else -> copy(teacher = teacher.trim(), campus = null)
    }
}
