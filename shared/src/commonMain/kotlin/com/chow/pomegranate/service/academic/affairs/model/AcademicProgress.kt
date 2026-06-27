package com.chow.pomegranate.service.academic.affairs.model

/**
 * 学业进度。
 *
 * @property userId 学号
 */
data class AcademicProgress(
    val userId: String,
    val isPrimary: Boolean,
    val modules: List<AcademicProgressModule>,
)

/**
 * 模块进度。
 *
 * @property name 模块名称
 * @property requiredCredits 模块应修学分
 * @property earnedCredits 已获学分
 * @property courses 课程进度
 */
data class AcademicProgressModule(
    val name: String,
    val requiredCredits: Double?,
    val earnedCredits: Double?,
    val courses: List<AcademicProgressCourse>,
)

/**
 * 课程进度。
 *
 * @property moduleName 课程模块
 * @property attribute 课程属性
 * @property courseId 课程编号
 * @property name 课程名称
 * @property credits 学分
 * @property suggestedSemesterString 建议修读学期，如 “1”、“1,2”
 * @property requiredCredits 模块应修学分
 * @property earnedCredits 已获学分
 */
data class AcademicProgressCourse(
    val moduleName: String,
    val attribute: String,
    val courseId: String,
    val name: String,
    val credits: Double,
    val suggestedSemesterString: String,
    val exemption: String?,
    val requiredCredits: Double?,
    val earnedCredits: Double?,
)