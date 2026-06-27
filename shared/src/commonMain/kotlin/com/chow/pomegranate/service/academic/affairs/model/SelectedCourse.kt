package com.chow.pomegranate.service.academic.affairs.model

import kotlinx.datetime.DayOfWeek

/**
 * 已选课程。
 *
 * @property id 选课操作 id
 * @property courseId 课程编号
 * @property name 课程名称
 * @property credits 学分
 * @property attribute 课程属性
 * @property teacher 教师
 * @property timeString 上课时间字符串
 * @property classroom 教室
 */
data class SelectedCourse(
    val id: String,
    val courseId: String,
    val name: String,
    val credits: Double,
    val attribute: String,
    val teacher: String,
    val timeString: String?,
    val classroom: String?,
) {
    private val timeParts by lazy {
        timeString?.split(" ")
    }

    /** 上课周次 */
    val weeksString: String?
        get() = timeParts?.getOrNull(0)

    /** 节次 */
    val sections: Set<Int>?
        get() = timeParts?.getOrNull(2)?.let { section ->
            Regex("\\d+")
                .findAll(section)
                .map { it.value.toInt() }
                .toList().sorted()
                // 有些多节次的课只会显示开始和结束，如 1-4 节，而不是 1-2-3-4 节
                .let { it[0]..it[1] }.toSet()
        }

    /** 星期 */
    val dayOfWeek: DayOfWeek?
        get() = timeParts?.getOrNull(1)?.let {
            DayOfWeek(
                when (it) {
                    "星期一" -> 1
                    "星期二" -> 2
                    "星期三" -> 3
                    "星期四" -> 4
                    "星期五" -> 5
                    "星期六" -> 6
                    else -> 7
                },
            )
        }
}