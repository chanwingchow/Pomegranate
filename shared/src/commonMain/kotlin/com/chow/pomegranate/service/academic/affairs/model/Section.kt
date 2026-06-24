package com.chow.pomegranate.service.academic.affairs.model

/**
 * 节次。
 */
enum class Section(private val duration: ClosedRange<Int>) {
    /** 1-2 */
    First(1..2),

    /** 3-4 */
    Second(3..4),

    /** 5-6 */
    Third(5..6),

    /** 7-8 */
    Fourth(7..8),

    /** 9-10 */
    Fifth(9..10),

    /** 11-12 */
    Sixth(11..12);


    override fun toString() = "${duration.start}-${duration.endInclusive}"
}