package com.chow.pomegranate.service.academic.affairs.demo

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.RoutedAcademicAffairs
import kotlinx.coroutines.delay
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlin.io.encoding.Base64
import kotlin.time.Clock

/** 本地教务服务。所有数据均为虚构，选退课仅修改内存，不发出网络请求。 */
class MockAcademicAffairs(
    responseDelayMillis: Long = 350L,
    courseTimetableDelayMillis: Long = 1_500L,
) : AcademicAffairs by delayedMock(responseDelayMillis, courseTimetableDelayMillis) {
    companion object {
        const val USER_ID = "演示账号"
    }
}

private fun delayedMock(responseDelayMillis: Long, courseTimetableDelayMillis: Long): AcademicAffairs {
    require(responseDelayMillis >= 0 && courseTimetableDelayMillis >= 0)
    val data = InMemoryAcademicAffairs()
    return RoutedAcademicAffairs { operation ->
        delay(if (operation == "enrollment.getCourseTimetable") courseTimetableDelayMillis else responseDelayMillis)
        data
    }
}

private class InMemoryAcademicAffairs : AcademicAffairs {
    private val USER_ID = MockAcademicAffairs.USER_ID

    private val semester = Semester.now()
    private val courses = listOf(
        TimetableCourse("数据结构", "林知远", "1-18周", "实验楼 A301", DayOfWeek.MONDAY, setOf(1, 2), "计算机演示班"),
        TimetableCourse("高等数学", "陈思雨", "1-18周", "教学楼 B202", DayOfWeek.MONDAY, setOf(5, 6)),
        TimetableCourse("大学英语", "周明", "1-18周", "教学楼 A105", DayOfWeek.TUESDAY, setOf(3, 4)),
        TimetableCourse("数据库原理", "林知远", "1-18周", "实验楼 A302", DayOfWeek.WEDNESDAY, setOf(1, 2)),
        TimetableCourse("经济学导论", "李若溪", "1-18周", "教学楼 C201", DayOfWeek.THURSDAY, setOf(7, 8)),
        TimetableCourse("大学体育", "王晨", "1-18周", "体育馆", DayOfWeek.FRIDAY, setOf(3, 4)),
    )
    private val teachers = listOf(
        BasicTeacher("demo-teacher-1", "林知远", "信息学院"),
        BasicTeacher("demo-teacher-2", "陈思雨", "数学学院"),
        BasicTeacher("demo-teacher-3", "周明", "外国语学院"),
        BasicTeacher("demo-teacher-4", "李若溪", "经济学院"),
        BasicTeacher("demo-teacher-5", "王晨", "体育学院"),
    )

    override val auth = object : AcademicAffairs.Auth {
        override suspend fun getCaptcha(): ByteArray = Base64.decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
        )
        override suspend fun login(param: LoginParam): LoginResult = LoginResult.Success
        override suspend fun logout() = Unit
    }

    override val enrollment = object : AcademicAffairs.Enrollment {
        override suspend fun getTimetable(semester: Semester) =
            Timetable(USER_ID, semester, courses, "演示数据，非真实教务记录。")

        override suspend fun getCourseTimetable(semester: Semester) = CourseTimetable(semester,
            (1..20).flatMap { classIndex ->
                courses.map { course -> course.copy(
                    clazz = "计算机演示 ${classIndex} 班",
                    classroom = "教学楼 ${('A'.code + (classIndex - 1) % 3).toChar()}${200 + classIndex}",
                ) }
            })

        override suspend fun getExamSchedule(semester: Semester): ExamSchedule {
            val year = semester.year + 1
            val month = if (semester.term == Semester.Term.First) 1 else 6
            return ExamSchedule(USER_ID, semester, courses.take(4).mapIndexed { index, course ->
                CourseExam("DEMO${index + 1}", course.name,
                    LocalDateTime(year, month, 8 + index, 9, 0),
                    LocalDateTime(year, month, 8 + index, 11, 0), Campus.Canton, "教学楼 B${201 + index}")
            })
        }

        override suspend fun getCourseTranscript() = CourseTranscript(
            USER_ID, "演示成绩 · 平均分 87.5 · 已获 15 学分",
            courses.take(4).mapIndexed { index, course ->
                CourseGrade(semester.previous(), "DEMO${index + 1}", course.name, "90", null,
                    (92 - index * 3).toString(), (92 - index * 3).toString(),
                    if (index == 0) 3.0 else 4.0, 64.0, "考试", "必修", "专业基础课", null, "正常考试", null, "演示数据")
            },
        )

        override suspend fun getLevelTranscript() = LevelTranscript(USER_ID, listOf(
            LevelGrade("大学英语四级（演示）", "568", LocalDate(semester.year, 6, 15)),
            LevelGrade("全国计算机等级二级（演示）", "优秀", LocalDate(semester.year, 3, 23)),
        ))

        override suspend fun getExemption(semester: Semester) = Exemption(USER_ID, semester, listOf(
            ExemptionItem("DEMO-EN", "大学英语", "外国语学院", "周明", 32.0, 2.0, "考试", "演示免听申请", "审核通过", LocalDate(semester.year, 9, 1), null),
        ))

        override suspend fun getCampusCalendar(semester: Semester): CampusCalendar {
            val start = if (semester.term == Semester.Term.First) LocalDate(semester.year, 9, 1)
                else LocalDate(semester.year + 1, 2, 20)
            return CampusCalendar(semester, start..start.plus(125, DateTimeUnit.DAY))
        }

        override suspend fun getTeachers(query: String, pageIndex: Int): Teachers {
            require(pageIndex >= 1)
            val filtered = teachers.filter { query.trim() in it.name || query.trim() in it.department.orEmpty() }
            val pageSize = 3
            return Teachers(pageIndex, maxOf(1, (filtered.size + pageSize - 1) / pageSize),
                filtered.drop((pageIndex - 1) * pageSize).take(pageSize))
        }

        override suspend fun getTeacher(id: String): Teacher {
            val teacher = teachers.firstOrNull { it.id == id } ?: error("演示教师不存在。")
            return Teacher(id, teacher.name, null, null, null, null, "副教授", "专任教师",
                teacher.department, "教学研究室", "研究生", "博士", "教学与研究（演示）", null, null, null, null,
                "这是一份虚构的教师资料，仅供功能演示。",
                listOf(TeacherCourse("数据结构", "专业必修", semester)), emptyList(), "知行合一", "保持好奇，持续学习。")
        }
    }

    override val academic = object : AcademicAffairs.Academic {
        override suspend fun getAcademicPlan() = AcademicPlan(USER_ID, courses.mapIndexed { index, course ->
            PlannedCourse(semester, "DEMO${index + 1}", course.name, "信息学院", 3.0, 48.0, "考试", "必修", true)
        })

        override suspend fun getAcademicProgress(isPrimary: Boolean) = AcademicProgress(USER_ID, isPrimary,
            listOf("通识教育课程", if (isPrimary) "专业基础课程" else "辅修专业课程", "实践教学").mapIndexed { index, name ->
                AcademicProgressModule(name, 30.0, 18.0 + index * 3, courses.take(2).mapIndexed { i, course ->
                    AcademicProgressCourse(name, "必修", "DEMO${i + 1}", course.name, 3.0, "1,2", null, 3.0, 3.0)
                })
            })

        override suspend fun getAcademicAlert(pageIndex: Int) = AcademicAlert(USER_ID, pageIndex, 1,
            if (pageIndex == 1) listOf(AcademicAlertItem(semester, AcademicAlertLevel.Low, "演示：请关注本学期选修学分完成情况", "还需 2 学分")) else emptyList())

        override suspend fun getAcademicProgram() = AcademicProgram(USER_ID, """
            <html><body><h1>计算机科学与技术专业培养方案（演示）</h1>
            <p>本页面内容为虚构数据，仅用于演示。</p><h2>培养目标</h2>
            <p>掌握计算机科学基础知识，具备软件设计、开发与实践能力。</p>
            <h2>学制与学分</h2><p>学制四年，毕业应修 160 学分。</p>
            <table border="1"><tr><th>课程模块</th><th>应修学分</th></tr>
            <tr><td>通识教育</td><td>40</td></tr><tr><td>专业课程</td><td>90</td></tr>
            <tr><td>实践教学</td><td>30</td></tr></table></body></html>
        """.trimIndent())

        override suspend fun getGraduationAudit(isPrimary: Boolean) = GraduationAudit(USER_ID, listOf(
            GraduationAuditItem(semester.year.toString(), "演示毕业审核", if (isPrimary) "主修" else "辅修",
                "自动审核", 3.72, "85%", "100%", "演示报告，无实际审核效力", "demo:graduation-report"),
        ))

        override suspend fun getGraduationAuditReport(urlString: String): ByteArray {
            require(urlString == "demo:graduation-report") { "演示报告不存在。" }
            return demoReport()
        }
    }

    override val courseSystem: AcademicAffairs.CourseSystem = DemoCourseSystem()

    private inner class DemoCourseSystem : AcademicAffairs.CourseSystem {
        private val mutex = Mutex()
        private val catalog = listOf(
            selectable("demo-select-1", "摄影与视觉表达", "李若溪", DayOfWeek.TUESDAY, 5, CourseSelectCategory.General),
            selectable("demo-select-2", "人工智能导论", "林知远", DayOfWeek.THURSDAY, 3, CourseSelectCategory.General),
            selectable("demo-select-3", "音乐鉴赏", "周明", DayOfWeek.FRIDAY, 7, CourseSelectCategory.General),
        ) + CourseSelectCategory.entries.filter { it != CourseSelectCategory.General }.map { category ->
            selectable("demo-${category.name}", "${category.name} 演示课程", "陈思雨", DayOfWeek.WEDNESDAY, 5, category)
        }
        private val selectedIds = mutableSetOf("demo-select-1")
        private val dropLogs = mutableListOf<CourseDropLog>()

        override suspend fun getEntrances() = listOf(CourseSystemEntrance(
            "demo-entrance", USER_ID, semester, "${semester} 演示选课",
            LocalDateTime(semester.year, 1, 1, 0, 0), LocalDateTime(semester.year + 1, 12, 31, 23, 59),
        ))

        override suspend fun enterCourseSystem(id: String) {
            require(id == "demo-entrance") { "演示选课批次不存在。" }
        }

        override suspend fun getCreditSummary() = mutex.withLock {
            CreditSummary("演示选退课仅影响虚构数据，退出应用后重置。", listOf(CreditSummary.CreditProgress(
                "通识选修", catalog.filter { it.id in selectedIds }.sumOf { it.credits }.toString(), "12.0")))
        }

        override suspend fun getSelectedCourses() = mutex.withLock {
            catalog.filter { it.id in selectedIds }.map {
                SelectedCourse(it.id, it.courseId, it.name, it.credits, "选修", it.teacher, it.schedule, it.classroom)
            }
        }

        override suspend fun getCourseDropLogs() = mutex.withLock { dropLogs.toList() }

        override suspend fun searchCourses(params: CourseSearchParam) = mutex.withLock {
            require(params.pageIndex >= 0 && params.pageSize > 0)
            val category = when (params) {
                is CourseSearchParam.Basic -> CourseSelectCategory.Basic
                is CourseSearchParam.Optional -> CourseSelectCategory.Optional
                is CourseSearchParam.General -> CourseSelectCategory.General
                is CourseSearchParam.Professional -> CourseSelectCategory.Professional
                is CourseSearchParam.CrossGrade -> CourseSelectCategory.CrossGrade
                is CourseSearchParam.Interprofessional -> CourseSelectCategory.Interprofessional
            }
            val available = catalog.filter { it.selectionCategoryCode == category.name }.map {
                val selected = it.id in selectedIds
                it.copy(isSelected = selected, remaining = it.remaining - if (selected) 1 else 0,
                    enrolled = it.enrolled + if (selected) 1 else 0)
            }
            val extra = params as? CourseSearchParam.CourseSearchExtraParam
            val filtered = available.filter { course ->
                extra == null || (
                    (extra.query.trim() in course.name || extra.query.trim() in course.teacher) &&
                    extra.teacher.trim() in course.teacher &&
                    (extra.dayOfWeek == null || course.schedules.any { it.dayOfWeek == extra.dayOfWeek?.ordinal?.plus(1) }) &&
                    (extra.section == null || course.schedules.any { it.sections == extra.section.toString() }) &&
                    (!extra.filterFull || !course.isFull) &&
                    (!extra.filterConflicts || course.conflictDescription.isBlank()) &&
                    (params !is CourseSearchParam.General || params.campus == null || params.campus == Campus.Canton)
                )
            }
            CourseSearchResponse(filtered.drop(params.pageIndex * params.pageSize).take(params.pageSize),
                null, available.size, filtered.size)
        }

        override suspend fun selectCourse(id: String, category: CourseSelectCategory, priority: CourseSelectPriority?) = mutex.withLock {
            val course = catalog.firstOrNull { it.id == id && it.selectionCategoryCode == category.name }
                ?: return@withLock CourseOperateResult(false, "演示课程不存在或分类不匹配。")
            if (!selectedIds.add(course.id)) CourseOperateResult(false, "已选过这门课程。")
            else CourseOperateResult(true, "演示选课成功。")
        }

        override suspend fun dropCourse(id: String) = mutex.withLock {
            val course = catalog.firstOrNull { it.id == id }
            if (course == null || !selectedIds.remove(id)) return@withLock CourseOperateResult(false, "尚未选择这门课程。")
            dropLogs.add(CourseDropLog(course.courseId, course.name, course.credits, "选修", course.teacher,
                listOf(course.schedule), "演示课程", "学生退课", Clock.System.now().toString(), USER_ID, "演示操作"))
            CourseOperateResult(true, "演示退课成功。")
        }
    }

    private fun selectable(id: String, name: String, teacher: String, day: DayOfWeek, firstSection: Int, category: CourseSelectCategory) = SelectableCourse(
        id = id, courseId = "DEMO-$id", name = name, credits = 2.0, totalHours = 32,
        teacher = teacher, teacherIds = "demo-teacher-1", schedule = "1-16周 星期${"一二三四五六日"[day.ordinal]} $firstSection-${firstSection + 1}节",
        classroom = "教学楼 A201", campusId = "2", schedules = listOf(SelectableCourse.ClassSchedule(
            (1..16).map(Int::toString), "1-16", day.ordinal + 1, "$firstSection-${firstSection + 1}", "教学楼 A201")),
        capacity = 40, remaining = 10, enrolled = 30, preselected = 0, scheduledCapacity = 40,
        selectionCategory = "演示课程", selectionCategoryCode = category.name, attributeCode = "2",
        department = "信息学院", offeringUnitCode = "demo", conflictDescription = "",
        isOpenForSelection = true, isSelected = false, isDeleted = false, semesterId = semester.toString(),
        courseMasterId = id, offeringCode = null, courseNatureCode = "2", examMethodCode = "1", adjustmentCategory = "0",
    )
}

/** 单页 ASCII PDF，供现有报告阅读器演示使用。 */
private fun demoReport(): ByteArray {
    val content = "BT /F1 18 Tf 50 760 Td (Demo Graduation Audit) Tj 0 -30 Td /F1 12 Tf (Fictional data - for demonstration only.) Tj ET"
    val objects = listOf(
        "<< /Type /Catalog /Pages 2 0 R >>",
        "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        "<< /Length ${content.length} >>\nstream\n$content\nendstream",
    )
    val pdf = StringBuilder("%PDF-1.4\n")
    val offsets = objects.mapIndexed { index, obj ->
        pdf.length.also { pdf.append("${index + 1} 0 obj\n$obj\nendobj\n") }
    }
    val xref = pdf.length
    pdf.append("xref\n0 6\n0000000000 65535 f \n")
    offsets.forEach { pdf.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
    pdf.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
    return pdf.toString().encodeToByteArray()
}
