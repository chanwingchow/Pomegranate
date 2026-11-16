package com.chow.pomegranate.ui.screen.main.timetable

import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import kotlinx.datetime.DayOfWeek

/** 课表行数：每两节一行，完整覆盖 12 节。 */
internal const val TIMETABLE_ROW_COUNT = 6

/** 周次上限，第 0 周表示开学前一周。 */
internal const val TIMETABLE_MAX_WEEK = com.chow.pomegranate.repository.MAX_ACADEMIC_WEEK + 1

/** 每行包含的节数。 */
private const val SECTIONS_PER_ROW = 2

/**
 * 课表展示模型。
 *
 * 把 [TimetableCourse] 按"星期 + 行"归位，便于网格直接渲染。
 *
 * @property courses 课程，键为行下标，值为该行 7 天的课程
 */
class TimetableDisplay(
    val courses: Map<Int, Map<DayOfWeek, List<TimetableCourse>>>,
) {
    /** 返回第 [row] 行 [dayOfWeek] 的课程。 */
    fun coursesAt(
        row: Int,
        dayOfWeek: DayOfWeek,
    ): List<TimetableCourse> = courses[row]?.get(dayOfWeek).orEmpty()

    companion object {
        /**
         * 由 [courses] 构建展示模型。
         *
         * @param week 当前周次（第 0 周为开学前一周），课程会按上课周次过滤
         */
        fun of(
            courses: List<TimetableCourse>,
            week: Int = 1,
            includeOtherWeeks: Boolean = false,
        ): TimetableDisplay {
            val grouped = mutableMapOf<Int, MutableMap<DayOfWeek, MutableList<TimetableCourse>>>()

            courses.forEach { course ->
                // 按选中周次过滤，避免显示整学期的课
                if (!includeOtherWeeks && !course.isInWeek(week)) return@forEach

                // 单节课和跨多个时间段的课都保留，落在覆盖的每一行。
                course.sections.filter { it in 1..TIMETABLE_ROW_COUNT * SECTIONS_PER_ROW }
                    .map { (it - 1) / SECTIONS_PER_ROW }.distinct().forEach { row ->
                        grouped.getOrPut(row) { mutableMapOf() }
                            .getOrPut(course.dayOfWeek) { mutableListOf() }.add(course)
                    }
            }

            return TimetableDisplay(grouped)
        }
    }
}

/**
 * 判断课程是否在 [week] 周上课。
 *
 * 参与计算的周次：第 0 周表示开学前一周，与设置页的周次含义保持一致。
 */
internal fun TimetableCourse.isInWeek(week: Int): Boolean {
    val weeks = parseWeeks(weeksString) ?: return true

    // 周次与界面数字一致；第 0 周为开学前，不应误算为第 1 教学周。
    val target = week

    if (target !in weeks.range) return false

    // 单双周限制
    return when (weeks.parity) {
        Parity.Odd -> target % 2 == 1
        Parity.Even -> target % 2 == 0
        null -> true
    }
}

/** 单双周。 */
internal enum class Parity {
    /** 单周 */
    Odd,

    /** 双周 */
    Even,
}

/**
 * 周次解析结果。
 *
 * @property range 周次范围
 * @property parity 单双周限制，`null` 表示每周都上
 */
internal data class WeeksRange(
    val range: IntRange,
    val parity: Parity?,
)

/** 匹配 `1-16周(单)`、`3-15周(双)`、`1-16周` 等常见写法。 */
private val WEEKS_REGEX = Regex("""(\d+)\s*-\s*(\d+)\s*周(?:\(?(单|双)\)?)?""")

/** 匹配单个周次写法，如 `5周`。 */
private val SINGLE_WEEK_REGEX = Regex("""(\d+)\s*周(?:\(?(单|双)\)?)?""")

/**
 * 解析 [weeksString] 为周次范围。
 *
 * 无法识别时返回 `null`，由调用方按"每周都上"处理，避免漏课。
 */
internal fun parseWeeks(weeksString: String): WeeksRange? {
    if (weeksString.isBlank()) return null

    WEEKS_REGEX.find(weeksString)?.let { match ->
        val (start, end, parity) = match.destructured

        val range = start.toInt()..end.toInt()
        if (range.isEmpty()) return null

        return WeeksRange(
            range = range,
            parity = parity.toParity(),
        )
    }

    SINGLE_WEEK_REGEX.find(weeksString)?.let { match ->
        val (week, parity) = match.destructured

        return WeeksRange(
            range = week.toInt()..week.toInt(),
            parity = parity.toParity(),
        )
    }

    return null
}

/** 把单双周文本转换为枚举。 */
private fun String.toParity(): Parity? = when (this) {
    "单" -> Parity.Odd
    "双" -> Parity.Even
    else -> null
}
