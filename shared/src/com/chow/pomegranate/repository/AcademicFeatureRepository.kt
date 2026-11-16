package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.BasicTeacher
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.ExtracurricularGrade
import com.chow.pomegranate.service.foundation.Semester
import org.koin.core.annotation.Single

/**
 * 学业功能。
 *
 * 不同的功能数据模型并不一样，把它提到类型上，调用方就必须显式处理两种取数方式，
 * 而不是在一个"返回全量"的函数签名里用占位分支堵住。
 */
enum class AcademicFeature(
    val dataModel: FeatureDataModel,
) {
    CourseSelection(FeatureDataModel.Whole),
    Extracurricular(FeatureDataModel.Whole),
    Teachers(FeatureDataModel.Paged),
    CourseTimetable(FeatureDataModel.Whole),
    AcademicPlan(FeatureDataModel.Whole),
    AcademicProgress(FeatureDataModel.Whole),
}

/** 功能的取数模型。 */
enum class FeatureDataModel {
    /** 一次请求返回全部数据。 */
    Whole,

    /** 服务端分页，客户端按页取。 */
    Paged,
}

/** 学业功能列表项。 */
data class AcademicFeatureItem(
    val id: String,
    val title: String,
    val supportingText: String,
    val details: List<Pair<String, String>>,
)

/**
 * 一页学业功能数据。
 *
 * @property items 当前页数据
 * @property hasMorePages 是否还有下一页
 */
data class AcademicFeaturePage(
    val items: List<AcademicFeatureItem>,
    val hasMorePages: Boolean,
)

/** 学业功能数据仓库。 */
@Single
class AcademicFeatureRepository(
    private val academicAffairs: AcademicAffairs,
    private val extracurricularActivity: ExtracurricularActivity,
) {
    /**
     * 返回 [feature] 的展示数据。
     *
     * 只处理 [FeatureDataModel.Whole] 的功能。
     *
     * @param pageIndex 页码，仅 [FeatureDataModel.Paged] 的功能有意义
     * @param query 关键词，仅分页功能会上送服务端过滤
     */
    suspend fun getItems(
        feature: AcademicFeature,
        pageIndex: Int = FIRST_PAGE_INDEX,
        query: String = "",
    ): AcademicFeaturePage {
        return when (feature.dataModel) {
            FeatureDataModel.Paged -> getPage(
                feature = feature,
                pageIndex = pageIndex,
                query = query,
            )

            // 一次返回全量的功能：忽略页码，服务端没有分页概念
            FeatureDataModel.Whole -> AcademicFeaturePage(
                items = getWholeItems(feature),
                hasMorePages = false,
            )
        }
    }

    /** 取一页分页功能的数据。 */
    private suspend fun getPage(
        feature: AcademicFeature,
        pageIndex: Int,
        query: String,
    ): AcademicFeaturePage = when (feature) {
        AcademicFeature.Teachers -> {
            // 查询交给服务端（接口本身就是姓名过滤 + 分页），客户端只取当前页。
            // 早先的实现在客户端把全部分页拉完再本地过滤：一次进页面就是几十个
            // 串行请求，还得靠页数上限兜底，反而会静默截断数据。
            val page = academicAffairs.enrollment.getTeachers(
                query = query.trim(),
                pageIndex = pageIndex,
            )

            AcademicFeaturePage(
                items = page.items.map(BasicTeacher::toFeatureItem),
                hasMorePages = page.hasMorePages,
            )
        }

        // 其余功能在枚举里声明为 Whole，这里不可达
        else -> error("$feature 不是分页功能")
    }

    /** 取一次返回全量的功能数据。 */
    private suspend fun getWholeItems(feature: AcademicFeature): List<AcademicFeatureItem> =
        when (feature) {
            AcademicFeature.CourseSelection -> {
                val entrance = academicAffairs.courseSystem.getEntrances()
                    .maxByOrNull { it.endTime }
                    ?: return emptyList()
                academicAffairs.courseSystem.enterCourseSystem(entrance.id)
                academicAffairs.courseSystem.getSelectedCourses()
                    .map { it.toFeatureItem(entrance.name) }
            }

            AcademicFeature.Extracurricular -> extracurricularActivity.achievement
                .getTranscript()
                .items
                .map(ExtracurricularGrade::toFeatureItem)

            AcademicFeature.CourseTimetable -> academicAffairs.enrollment
                .getCourseTimetable(Semester.now())
                .courses
                .mapIndexed { index, course ->
                    AcademicFeatureItem(
                        id = "$index-${course.name}",
                        title = course.name,
                        supportingText = listOfNotNull(course.teacher, course.classroom)
                            .joinToString(" · "),
                        details = listOf(
                            "教师" to course.teacher.orEmpty(),
                            "周次" to course.weeksString,
                            "星期" to course.dayOfWeek.toString(),
                            "节次" to course.sections.sorted().joinToString(),
                            "教室" to course.classroom.orEmpty(),
                            "班级" to course.clazz.orEmpty(),
                        ),
                    )
                }

            AcademicFeature.AcademicPlan -> academicAffairs.academic
                .getAcademicPlan()
                .courses
                .map { course ->
                    AcademicFeatureItem(
                        id = "${course.semester}-${course.courseId}",
                        title = course.name,
                        supportingText = "${course.semester} · ${course.credits} 学分",
                        details = listOf(
                            "课程编号" to course.courseId,
                            "开课单位" to course.department,
                            "课程属性" to course.attribute,
                            "考核方式" to course.assessment,
                            "总学时" to course.hours.toString(),
                        ),
                    )
                }

            // 分页功能不会走到这里（见 dataModel 分支）
            AcademicFeature.Teachers -> error("教师列表是分页功能，应走 getPage")

            AcademicFeature.AcademicProgress -> academicAffairs.academic
                .getAcademicProgress()
                .modules
                .mapIndexed { index, module ->
                    AcademicFeatureItem(
                        id = "$index-${module.name}",
                        title = module.name,
                        supportingText = buildString {
                            append(module.earnedCredits ?: 0.0)
                            append(" / ")
                            append(module.requiredCredits ?: 0.0)
                            append(" 学分")
                        },
                        details = listOf(
                            "已获学分" to (module.earnedCredits ?: 0.0).toString(),
                            "要求学分" to (module.requiredCredits ?: 0.0).toString(),
                            "课程数量" to module.courses.size.toString(),
                        ),
                    )
                }
        }
}

/** 将真实已选课程转换为 UI 列表项。 */
internal fun SelectedCourse.toFeatureItem(batchName: String) = AcademicFeatureItem(
    id = id,
    title = name,
    supportingText = listOf(teacher, classroom)
        .filterNot { it.isNullOrBlank() }
        .joinToString(" · "),
    details = listOf(
        "选课批次" to batchName,
        "课程编号" to courseId,
        "学分" to credits.toString(),
        "课程属性" to attribute,
        "教师" to teacher,
        "上课时间" to timeString.orEmpty(),
        "教室" to classroom.orEmpty(),
    ),
)

/** 将第二课堂成绩转换为 UI 列表项。 */
internal fun ExtracurricularGrade.toFeatureItem() = AcademicFeatureItem(
    id = categoryId.toString(),
    title = name,
    supportingText = "$score / $requiredScore",
    details = listOf(
        "已获分数" to score.toString(),
        "要求分数" to requiredScore.toString(),
        "分数上限" to totalScore.toString(),
        "活动分数" to activityScore.toString(),
        "项目分数" to projectScore.toString(),
        "奖励分数" to awardScore?.toString().orEmpty(),
        "平均分" to averageScore?.toString().orEmpty(),
        "学期最低分" to termMinScore?.toString().orEmpty(),
    ),
)

/** 将教师转换为 UI 列表项。 */
internal fun BasicTeacher.toFeatureItem() = AcademicFeatureItem(
    id = id,
    title = name,
    supportingText = department.orEmpty(),
    details = listOf(
        "工号" to id,
        "院系" to department.orEmpty(),
    ),
)

/** 分页功能的起始页码。 */
private const val FIRST_PAGE_INDEX = 1
