package com.chow.pomegranate.service.academic.affairs.model

/**
 * 选课志愿。
 */
enum class CourseSelectPriority(
    val id: Int,
) {
    /** 第一志愿 */
    First(1),

    /** 第二志愿 */
    Second(2),

    /** 第三志愿 */
    Third(3),
}