package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Instant

data class StudyGroup(val title: String, val records: List<StudyRecord>)
data class StudyCatalog(val groups: List<StudyGroup>, val hasMore: Boolean = false, val schoolTimetable: CourseTimetable? = null)

internal fun fields(vararg values: Pair<String, String?>) = values.mapNotNull { (label, value) ->
    value?.takeIf { it.isNotBlank() }?.let { StudyField(label, it) }
}
internal fun credit(value: Double?) = value?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "—"

/** 按功能保留学期、课次、模块及明细层级，详情不再丢失为一个字段表。 */
@Single
class StudyCatalogRepository(
    private val academic: AcademicAffairs,
    private val extracurricular: ExtracurricularActivity,
) {
    suspend fun load(feature: AcademicFeature, semester: Semester, query: String = "", page: Int = 1, tab: Int = 0): StudyCatalog = when (feature) {
        AcademicFeature.CourseTimetable -> academic.enrollment.getCourseTimetable(semester).toStudyCatalog()
        AcademicFeature.Teachers -> {
            val result = academic.enrollment.getTeachers(query, page)
            StudyCatalog(listOf(StudyGroup("教师", result.items.map { teacher ->
                StudyRecord("teacher:${teacher.id}", teacher.name, listOfNotNull(teacher.department?.takeIf { it.isNotBlank() }, "工号 ${teacher.id}").joinToString(" · "),
                    detail = StudyDetailRoute("teacher:${teacher.id}", teacher.name, teacher.department.orEmpty(), teacherId = teacher.id))
            })), result.hasMorePages)
        }
        AcademicFeature.AcademicPlan -> StudyCatalog(academic.academic.getAcademicPlan().courses.groupBy { it.semester }.toSortedMap().map { (term, courses) ->
            StudyGroup(term.toString(), courses.map { course ->
                val id = "plan:$term:${course.courseId}"
                val sections = listOf(StudySection("课程信息", fields("课程编号" to course.courseId, "学分" to credit(course.credits),
                    "总学时" to credit(course.hours), "课程属性" to course.attribute, "考核方式" to course.assessment, "开课单位" to course.department)))
                StudyRecord(id, course.name, "${course.attribute} · ${credit(course.hours)} 学时 · ${course.assessment}", course.department,
                    value = "${credit(course.credits)} 学分", detail = StudyDetailRoute(id, course.name, term.toString(), sections))
            })
        })
        AcademicFeature.AcademicProgress -> {
            val modules = academic.academic.getAcademicProgress().modules
            val categories = modules.flatMap { it.courses }.groupBy { "${it.moduleName} · ${it.attribute}" }.map { (name, courses) ->
                AcademicProgressModule(name, courses.firstNotNullOfOrNull { it.requiredCredits }, courses.sumOf { it.earnedCredits ?: 0.0 }, courses)
            }
            StudyCatalog(listOf(StudyGroup("模块总览", modules.mapIndexed { index, module -> module.record("module:$index") }),
                StudyGroup("课程类别", categories.mapIndexed { index, module -> module.record("category:$index") })))
        }
        AcademicFeature.Extracurricular -> StudyCatalog(listOf(when (tab) {
            0 -> StudyGroup("成绩", extracurricular.achievement.getTranscript().items.sortedBy { it.score >= it.requiredScore }.map { grade ->
                val id = "extracurricular:grade:${grade.categoryId}"
                StudyRecord(id, grade.name, if (grade.score >= grade.requiredScore) "已达到要求" else "尚未达到要求",
                    value = "${credit(grade.score)} / ${credit(grade.requiredScore)} 分",
                    progress = grade.requiredScore.takeIf { it > 0 }?.let { (grade.score / it).toFloat() },
                    achievement = StudyAchievement(grade.score, grade.requiredScore),
                    detail = StudyDetailRoute(id, grade.name, "第二课堂成绩", listOf(StudySection("成绩构成", fields(
                        "已获分数 / 要求分数" to "${credit(grade.score)} / ${credit(grade.requiredScore)}",
                        "活动分数" to credit(grade.activityScore), "项目分数" to credit(grade.projectScore),
                        "奖励分数" to grade.awardScore?.let(::credit), "分数上限" to credit(grade.totalScore),
                        "学期最低分数" to grade.termMinScore?.let(::credit))))))
            })
            1 -> StudyGroup("活动", extracurricular.activity.getEvents().map { event ->
                val id = "extracurricular:event:${event.id}"
                StudyRecord(id, event.name, supporting = "${dateTime(event.startTime)} · ${if (event.isOnline) "线上" else "线下"}",
                    tags = listOf(StudyRecordTag(event.category), StudyRecordTag("${credit(event.score)} 分", isScore = true)),
                    imageUrl = event.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        com.chow.pomegranate.service.utils.resolveUrl(url) { io.ktor.http.Url("http://2ketang.gdufe.edu.cn/") }
                    },
                    detail = StudyDetailRoute(id, event.name, event.organizer, eventId = event.id))
            })
            else -> StudyGroup("记录", extracurricular.achievement.getTranscriptLogs().items.mapIndexed { index, log ->
                val id = "extracurricular:log:$index"
                StudyRecord(id, log.displayName, supporting = dateTime(log.time),
                    tags = listOf(StudyRecordTag(log.category), StudyRecordTag("+${credit(log.score)} 分", isScore = true)),
                    detail = StudyDetailRoute(id, log.displayName, log.semester, listOf(StudySection("获取记录", fields(
                        "分类" to log.category, "分数" to credit(log.score), "获得时间" to dateTime(log.time), "备注" to log.note)))))
            })
        }))
        AcademicFeature.CourseSelection -> error("选课使用独立流程")
    }

    suspend fun teacher(id: String): List<StudySection> = academic.enrollment.getTeacher(id).let { teacher ->
        buildList {
            add(StudySection("联系方式", fields("电话" to teacher.phoneNumber, "邮箱" to teacher.email, "QQ" to teacher.qq, "微信" to teacher.weChat)))
            add(StudySection("教师资料", fields("工号" to teacher.id, "院系" to teacher.department, "科室" to teacher.office,
                "职称" to teacher.title, "职务" to teacher.duty, "教职工类别" to teacher.staffCategory,
                "学历" to teacher.qualification, "学位" to teacher.degree, "研究方向" to teacher.researchField,
                "性别" to teacher.gender, "政治面貌" to teacher.politics, "民族" to teacher.nation)))
            if (teacher.recentCourses.isNotEmpty()) add(StudySection("近四学期主讲课程", teacher.recentCourses.map { StudyField(it.name, "${it.category} · ${it.semester}", courseLabel = true) }))
            if (teacher.plannedCourses.isNotEmpty()) add(StudySection("计划开设课程", teacher.plannedCourses.map { StudyField(it.name, "${it.category} · ${it.semester}", courseLabel = true) }))
            add(StudySection("关于教师", fields("个人简介" to teacher.biography, "教学理念" to teacher.philosophy, "寄语" to teacher.slogan)))
        }.filter { it.fields.isNotEmpty() }
    }

    suspend fun event(id: Int): List<StudySection> = extracurricular.activity.getEvent(id).let { event ->
        listOf(StudySection("活动安排", fields("开始时间" to event.startTime, "结束时间" to event.endTime,
            "地点" to event.location, "分类" to event.category, "分数" to credit(event.score), "活动类型" to event.eventType)),
            StudySection("参与信息", fields("报名截止" to event.enrollEndTime, "已报名 / 名额" to "${event.participantCount} / ${event.peopleLimit}",
                "培训学时" to credit(event.trainingHours), "需要提交作业" to if (event.needSubmit) "是" else "否",
                "主办方" to event.organizer, "协办方" to event.coOrganizer, "管理员" to event.admin, "联系电话" to event.phoneNumber,
                "指导教师" to event.teacher)), StudySection("活动介绍", fields("" to event.description, "备注" to event.note)))
    }
}

internal fun AcademicProgressModule.record(key: String): StudyRecord {
    val id = "progress:$key:$name"
    val detail = StudyAcademicProgress(earnedCredits, requiredCredits, courses.map { course ->
        StudyProgressCourse(course.courseId, course.name, course.attribute, course.credits,
            course.earnedCredits, course.suggestedSemesterString, course.exemption)
    })
    return StudyRecord(id, name, "${courses.size} 门课程", value = "${credit(earnedCredits)} / ${credit(requiredCredits)} 学分",
        progress = requiredCredits?.takeIf { it > 0 }?.let { ((earnedCredits ?: 0.0) / it).toFloat() },
        achievement = StudyAchievement(earnedCredits, requiredCredits, "学分"),
        detail = StudyDetailRoute(id, name, "${credit(earnedCredits)} / ${credit(requiredCredits)} 学分", academicProgress = detail))
}

private fun dateTime(epoch: Long) = Instant.fromEpochMilliseconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault()).let {
    "${it.date} ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
}

/** 先筛课次、再按课程合并；保留原始课表以便搜索、筛选和刷新复用同一份数据。 */
internal fun CourseTimetable.toStudyCatalog(matches: List<TimetableCourse> = courses): StudyCatalog =
    StudyCatalog(listOf(StudyGroup("课程", matches.groupBy { it.name }.toSortedMap().map { (name, courses) ->
        val teachers = courses.mapNotNull { it.teacher }.distinct().joinToString("、")
        val sections = courses.sortedWith(compareBy({ it.dayOfWeek.ordinal }, { it.sections.minOrNull() ?: 0 }))
            .mapIndexed { index, course -> StudySection("课次 ${index + 1}",
                listOfNotNull(course.teacher?.takeIf { it.isNotBlank() }?.let { StudyField("教师", it, teacherQuery = it) }) + fields(
                "时间" to "${course.weeksString} · 星期${"一二三四五六日"[course.dayOfWeek.ordinal]} · ${course.sections.sorted().joinToString("、")}节",
                "教室" to course.classroom, "班级" to course.clazz)) }
        StudyRecord("timetable:$name", name, teachers, "${courses.size} 个课次",
            detail = StudyDetailRoute("timetable:$name", name, "$semester · ${courses.size} 个课次", sections))
    })), schoolTimetable = this)
