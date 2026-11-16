package com.chow.pomegranate.service.foundation

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 学期。
 *
 * @property year 学年起始年
 * @property term 学期分段
 */
data class Semester(
    val year: Int,
    val term: Term,
) : Comparable<Semester> {
    /** 线性序号，用于比较与 +1 / -1 切换学期。 */
    private val index: Int
        get() = year * 2 + term.ordinal

    override fun toString(): String = "$year-${year + 1}-${term.number}"

    override fun compareTo(other: Semester): Int =
        index.compareTo(other.index)

    /** 上一学期。 */
    fun previous(): Semester = fromIndex(index - 1)

    /** 下一学期。 */
    fun next(): Semester = fromIndex(index + 1)

    companion object {
        /** 学期解析正则 */
        private val formatRegex = Regex("""^(\d{4})-(\d{4})-([12])$""")

        /**
         * 返回当前学期。
         */
        fun now(): Semester {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val year = now.year
            return when {
                // 1.1 - 1.20
                now < LocalDateTime(year, 1, 20, 0, 0) -> Semester(
                    year - 1,
                    Term.First,
                )
                // 1.20 - 7.20
                now < LocalDateTime(year, 7, 20, 0, 0) -> Semester(
                    year - 1,
                    Term.Second,
                )
                // 7.20 - 12.31
                else -> Semester(
                    year,
                    Term.First,
                )
            }
        }

        /**
         * 将 [input] 解析为 [Semester]。
         *
         * @param input 学期字符串，格式为 “yyyy-yyyy-Term”
         */
        fun parse(input: String): Semester {
            val matchResult = formatRegex.matchEntire(input)
                ?: error("Invalid semester input: $input.")

            val (startYearStr, endYearStr, termNumberStr) = matchResult.destructured

            val startYear = startYearStr.toInt()
            val endYear = endYearStr.toInt()

            // 起止年份应该相差 1
            check(startYear == endYear - 1) { "Invalid semester input: $input." }

            val term = when (termNumberStr.toInt()) {
                Term.First.number -> Term.First
                Term.Second.number -> Term.Second
                else -> error("Invalid semester input: $input.")
            }

              return Semester(startYear, term)
        }

        private fun fromIndex(index: Int): Semester {
            return Semester(
                year = index / 2,
                term = Term.entries[index % 2],
            )
        }
    }

    /**
     * 学期分段。
     */
    enum class Term(val number: Int) {
        /** 上学期 */
        First(1),

        /** 下学期 */
        Second(2);
    }
}