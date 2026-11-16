package com.chow.pomegranate.ui.screen.main.timetable

import com.chow.pomegranate.service.foundation.Campus

/** 沿用 Punica 的分校区作息，按每一节记录，涵盖 1–12 节。 */
internal fun sectionTimeRanges(campus: Campus): List<Pair<String, String>> = when (campus) {
    Campus.Canton -> listOf("08:00" to "08:45", "08:55" to "09:40", "10:00" to "10:45", "10:55" to "11:40",
        "14:10" to "14:55", "15:05" to "15:50", "16:10" to "16:55", "17:05" to "17:50",
        "18:40" to "19:25", "19:35" to "20:20", "20:30" to "21:15", "21:25" to "22:10")
    Campus.Foshan -> listOf("08:30" to "09:15", "09:15" to "10:00", "10:20" to "11:05", "11:05" to "11:50",
        "14:00" to "14:45", "14:45" to "15:30", "15:50" to "16:35", "16:35" to "17:20",
        "18:30" to "19:15", "19:15" to "20:00", "20:20" to "21:05", "21:05" to "21:50")
}

internal fun timetableTimeFor(sections: List<Int>, campus: Campus = Campus.Canton): String? {
    val start = sections.minOrNull() ?: return null
    val end = sections.maxOrNull() ?: return null
    if (sections.toSet() != (start..end).toSet()) return null
    val times = sectionTimeRanges(campus)
    val from = times.getOrNull(start - 1)?.first ?: return null
    val to = times.getOrNull(end - 1)?.second ?: return null
    return "$from–$to"
}
