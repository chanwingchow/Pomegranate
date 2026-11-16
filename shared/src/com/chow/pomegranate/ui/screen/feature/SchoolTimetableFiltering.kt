package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.service.academic.affairs.model.Section
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import com.chow.pomegranate.repository.ClassroomReplacements
import com.chow.pomegranate.repository.NameReplacementTarget
import com.chow.pomegranate.ui.screen.main.timetable.isInWeek
import com.chow.pomegranate.ui.screen.main.timetable.parseWeeks
import kotlinx.serialization.Serializable

@Serializable
data class SchoolTimetableFilters(
    val teacher: String = "", val classroom: String = "", val clazz: String = "",
    val weekday: Int? = null, val section: Section? = null, val week: Int? = null,
) {
    fun normalized() = copy(teacher = teacher.trim(), classroom = classroom.trim(), clazz = clazz.trim(),
        weekday = weekday?.takeIf { it in 1..7 }, week = week?.takeIf { it in 1..30 })

    val isActive: Boolean get() = normalized() != SchoolTimetableFilters()

    val summary: String get() = buildList {
        teacher.trim().takeIf(String::isNotEmpty)?.let(::add)
        classroom.trim().takeIf(String::isNotEmpty)?.let(::add)
        clazz.trim().takeIf(String::isNotEmpty)?.let(::add)
        weekday?.takeIf { it in 1..7 }?.let { add("星期${"一二三四五六日"[it - 1]}") }
        section?.let { add("$it 节") }
        week?.let { add("第 $it 周") }
    }.joinToString(" · ").ifBlank { "星期、节次、周次、教师、教室、班级" }
}

@Serializable
data class SchoolTimetableFilterOptions(
    val teachers: List<String>, val classrooms: List<String>, val classes: List<String>, val maxWeek: Int,
) {
    companion object {
        fun from(courses: List<TimetableCourse>) = SchoolTimetableFilterOptions(
            courses.mapNotNull { it.teacher?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted(),
            courses.mapNotNull { it.classroom?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted(),
            courses.mapNotNull { it.clazz?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted(),
            (courses.mapNotNull { parseWeeks(it.weeksString)?.range?.last }.maxOrNull() ?: 20).coerceIn(1, 30),
        )
    }
}

/** 所有条件必须命中同一课次，防止同名课程的不同教师/班级交叉匹配。 */
internal fun List<TimetableCourse>.filterSchoolTimetable(query: String, filters: SchoolTimetableFilters,
    classrooms: ClassroomReplacements = ClassroomReplacements(),
    courseNames: ClassroomReplacements = ClassroomReplacements(target = NameReplacementTarget.Course)): List<TimetableCourse> {
    val f = filters.normalized()
    val keyword = query.trim()
    val sectionStart = f.section?.ordinal?.times(2)?.plus(1)
    return filter { course ->
        (keyword.isBlank() || listOf(course.name, course.teacher.orEmpty(), course.classroom.orEmpty(), course.clazz.orEmpty(),
                "${course.weeksString} · 星期${"一二三四五六日"[course.dayOfWeek.ordinal]} · ${course.sections.sorted().joinToString("、")}节")
            .any { it.contains(keyword, ignoreCase = true) } || classrooms.matches(course.classroom.orEmpty(), keyword) || courseNames.matches(course.name, keyword)) &&
            course.teacher.orEmpty().contains(f.teacher, ignoreCase = true) &&
            classrooms.matches(course.classroom.orEmpty(), f.classroom) &&
            course.clazz.orEmpty().contains(f.clazz, ignoreCase = true) &&
            (f.weekday == null || course.dayOfWeek.ordinal + 1 == f.weekday) &&
            (sectionStart == null || course.sections.any { it in sectionStart..sectionStart + 1 }) &&
            (f.week == null || course.isInWeek(f.week))
    }
}
