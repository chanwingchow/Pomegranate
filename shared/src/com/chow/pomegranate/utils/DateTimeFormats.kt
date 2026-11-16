package com.chow.pomegranate.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char

/** 月-日，如 09-02 */
val monthDayFormat by lazy {
    LocalDate.Format {
        monthNumber()
        char('-')
        day()
    }
}

/** 时:分，如 09:00 */
val timeFormat by lazy {
    LocalTime.Format {
        hour()
        char(':')
        minute()
    }
}
