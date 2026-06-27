package com.chow.pomegranate.service.academic.affairs.model

import com.chow.pomegranate.service.foundation.Semester
import kotlinx.datetime.LocalDate

/**
 * 校历。
 *
 * @property semester 学期
 * @property duration 时间范围
 */
data class CampusCalendar(
    val semester: Semester,
    val duration: ClosedRange<LocalDate>,
)