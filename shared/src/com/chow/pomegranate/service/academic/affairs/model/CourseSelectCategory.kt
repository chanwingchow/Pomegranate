package com.chow.pomegranate.service.academic.affairs.model

/**
 * 选课课程分类。
 *
 * @property searchRoute 搜索课程路由
 * @property operateRoute 操作（选课退课）路由
 */
enum class CourseSelectCategory(
    val searchRoute: String,
    val operateRoute: String = searchRoute.lowercase(),
) {
    /** 学科基础、专业必修课 */
    Basic("Bx"),

    /** 选修课 */
    Optional("Xx"),

    /** 通识课 */
    General("Ggxxk"),

    /** 专业内计划课 */
    Professional("Bxqjh"),

    /** 跨年级 */
    CrossGrade("Knj"),

    /** 跨专业 */
    Interprofessional("Faw");
}