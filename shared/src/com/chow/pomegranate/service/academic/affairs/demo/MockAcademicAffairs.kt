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
        TimetableCourse("高等数学", "陈思雨", "1-17周(单)", "教学楼 B202", DayOfWeek.MONDAY, setOf(5, 6)),
        TimetableCourse("大学英语", "周明", "1-18周", "教学楼 A105", DayOfWeek.TUESDAY, setOf(3, 4)),
        TimetableCourse("数据库原理", "林知远", "1-17周(单)", "乐学楼503", DayOfWeek.WEDNESDAY, setOf(1, 2)),
        TimetableCourse("计算机网络", "林知远", "2-18周(双)", "尚学楼503", DayOfWeek.WEDNESDAY, setOf(1, 2)),
        TimetableCourse("经济学导论", "李若溪", "1-18周", "教学楼 C201", DayOfWeek.THURSDAY, setOf(7, 8)),
        TimetableCourse("毛泽东思想和中国特色社会主义理论体系概论", "李若溪", "1-18周", "教学楼 C305", DayOfWeek.THURSDAY, setOf(5, 6)),
        TimetableCourse("大学体育", "王晨", "1-18周", "体育馆", DayOfWeek.FRIDAY, setOf(3, 4)),
    )
    private val teachers = listOf(
        BasicTeacher("demo-teacher-1", "林知远", "信息学院"),
        BasicTeacher("demo-teacher-2", "陈思雨", "数学学院"),
        BasicTeacher("demo-teacher-3", "周明", "外国语学院"),
        BasicTeacher("demo-teacher-4", "李若溪", "经济学院"),
        BasicTeacher("demo-teacher-5", "王晨", "体育学院"),
    ) + listOf("赵", "吴", "郑", "孙", "何", "许", "曹", "蒋", "沈", "韩", "唐", "罗").flatMapIndexed { family, surname ->
        listOf("知行", "雨桐", "景川", "安宁", "思远").mapIndexed { index, name ->
            BasicTeacher("demo-teacher-${6 + family * 5 + index}", surname + name, demoDepartments[(family + index) % demoDepartments.size])
        }
    }

    override val auth = object : AcademicAffairs.Auth {
        override suspend fun getCaptcha(): ByteArray = Base64.decode(
            "iVBORw0KGgoAAAANSUhEUgAAAGgAAAAoCAIAAACdJiiaAAAAq0lEQVR4nO3VQQrCMBgF4V7ItXfp/e+hC6EUQiBv2ugfmSHbPOO36fYy1PbrB6yacDDhYMLBhIMFcI/nfhx2q3e+uXD9H30STrj85cLVg2MPHen6mnDCJQkH+3O4u35mxppwwiUJBxMuuMU+NRUW2oQTbuZCm3ArwKWPm70snHBJwsGEgwkHEw4mHKw03Hm6d+ovjCynd4UTLk+4enDWSziYcDDhYMLBhIO9AYb61KgeN5QvAAAAAElFTkSuQmCC",
        )
        override suspend fun login(param: LoginParam): LoginResult = LoginResult.Success
        override suspend fun logout() = Unit
    }

    override val enrollment = object : AcademicAffairs.Enrollment {
        override suspend fun getCourseTimetableSemesters(): List<Semester> =
            (semester.year - 5..semester.year).flatMap { year -> Semester.Term.entries.map { Semester(year, it) } }
                .filter { it <= semester.next() }.sortedDescending()

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
            val past = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(7, DateTimeUnit.DAY)
            val first = if (semester == this@InMemoryAcademicAffairs.semester) past.plus(14, DateTimeUnit.DAY) else LocalDate(year, month, 8)
            return ExamSchedule(USER_ID, semester, demoCourseNames.take(24).mapIndexed { index, name ->
                val date = first.plus(index / 2, DateTimeUnit.DAY)
                val hour = if (index % 2 == 0) 9 else 14
                CourseExam("DEMO${index + 1}", name, LocalDateTime(date, LocalTime(hour, 0)),
                    LocalDateTime(date, LocalTime(hour + 2, 0)), Campus.Canton, "教学楼 B${201 + index}")
            } + (0 until 12).map { index ->
                val date = past.minus(index, DateTimeUnit.DAY)
                CourseExam(if (index == 0) "DEMO-FINISHED" else "DEMO-FINISHED-$index",
                    if (index == 0) "大学英语阶段测验" else "${demoCourseNames[index]}阶段测验",
                    LocalDateTime(date, LocalTime(9, 0)), LocalDateTime(date, LocalTime(11, 0)), Campus.Canton, "教学楼 A${105 + index}")
            })
        }

        override suspend fun getCourseTranscript(): CourseTranscript {
            val grades = demoCourseNames.mapIndexed { index, name ->
                val term = when (index / 20) { 0 -> semester; 1 -> semester.previous(); else -> semester.previous().previous() }
                val score = when (index) { 0 -> "54"; 1 -> "58"; 5 -> "不及格"; 9 -> "46"; else -> (98 - index % 14 * 2).toString() }
                CourseGrade(term, "DEMO${index + 1}", name, "90", null, score, score,
                    3.0, 48.0, if (index % 4 == 0) "考查" else "考试", if (index % 5 == 0) "选修" else "必修",
                    if (index < 20) "专业基础课" else "通识教育课程", null, "正常考试", null, "演示数据")
            }
            val attempts = listOf(
                grades[0].copy(score = "76", finalScore = "76", examCategory = "补考"),
                grades[1].copy(score = "83", finalScore = "83", examCategory = "重修"),
                grades[9].copy(score = "57", finalScore = "57", examCategory = "补考"),
                grades[22].copy(semester = semester, score = "52", finalScore = "52", examCategory = "重修"),
            )
            return CourseTranscript(USER_ID, "共 60 门课程，包含正常考试、补考和重修记录。演示成绩仅用于界面预览，不代表实际学业情况。", grades + attempts)
        }

        override suspend fun getLevelTranscript() = LevelTranscript(USER_ID, (0 until 18).map { index ->
            val name = listOf("大学英语四级", "大学英语六级", "全国计算机等级二级", "全国计算机等级三级", "普通话水平测试", "商务英语")[index % 6]
            LevelGrade("$name（演示）", if (index % 6 < 2) (568 - index * 3).toString() else "优秀",
                LocalDate(semester.year - index / 6, if (index % 2 == 0) 6 else 12, 15))
        })

        override suspend fun getExemption(semester: Semester) = Exemption(USER_ID, semester,
            demoCourseNames.take(20).mapIndexed { index, name ->
                ExemptionItem("DEMO-EX-$index", name, demoDepartments[index % demoDepartments.size], teachers[index % teachers.size].name,
                    32.0, 2.0, "考试", "演示免听申请", if (index % 3 == 0) "审核中" else "审核通过", LocalDate(semester.year, 9, 1), null)
            })

        override suspend fun getCampusCalendar(semester: Semester): CampusCalendar {
            val start = if (semester.term == Semester.Term.First) LocalDate(semester.year, 9, 1)
                else LocalDate(semester.year + 1, 2, 20)
            return CampusCalendar(semester, start..start.plus(125, DateTimeUnit.DAY))
        }

        override suspend fun getTeachers(query: String, pageIndex: Int): Teachers {
            require(pageIndex >= 1)
            val filtered = teachers.filter { query.trim() in it.name || query.trim() in it.department.orEmpty() }
            val pageSize = 12
            return Teachers(pageIndex, maxOf(1, (filtered.size + pageSize - 1) / pageSize),
                filtered.drop((pageIndex - 1) * pageSize).take(pageSize))
        }

        override suspend fun getTeacher(id: String): Teacher {
            val teacher = teachers.firstOrNull { it.id == id } ?: error("演示教师不存在。")
            return Teacher(id, teacher.name, null, null, null, null, "副教授", "专任教师",
                teacher.department, "教学研究室", "研究生", "博士", "教学与研究（演示）", null, null, null, null,
                "这是一份虚构的教师资料，仅供功能演示。",
                demoCourseNames.take(24).mapIndexed { index, name -> TeacherCourse(name, if (index % 3 == 0) "通识选修" else "专业必修", if (index < 12) semester else semester.previous()) }, emptyList(), "知行合一", "保持好奇，持续学习。")
        }
    }

    override val academic = object : AcademicAffairs.Academic {
        override suspend fun getAcademicPlan() = AcademicPlan(USER_ID, demoCourseNames.mapIndexed { index, name ->
            PlannedCourse(when (index / 20) { 0 -> semester.previous(); 1 -> semester; else -> semester.next() }, "DEMO${index + 1}", name, demoDepartments[index % demoDepartments.size], 3.0, 48.0, "考试", "必修", true)
        })

        override suspend fun getAcademicProgress(isPrimary: Boolean) = AcademicProgress(USER_ID, isPrimary,
            listOf("通识教育课程", if (isPrimary) "专业基础课程" else "辅修专业课程", "专业核心课程", "专业方向课程", "跨学科课程", "创新创业", "实践教学", "毕业论文与设计").mapIndexed { index, name ->
                AcademicProgressModule(name, 72.0, 18.0 + index * 3, (0 until 24).map { i ->
                    AcademicProgressCourse(name, if (i < 6) "必修" else "选修", "DEMO-$index-$i",
                        demoCourseNames[(index * 7 + i) % demoCourseNames.size], 3.0, "${i / 2 + 1}", null,
                        if (i < 6) 18.0 else 54.0, if (i < 6 + index) 3.0 else 0.0)
                })
            })

        override suspend fun getAcademicAlert(pageIndex: Int): AcademicAlert {
            require(pageIndex >= 1)
            val items = (0 until 30).map { index -> AcademicAlertItem(semester,
                AcademicAlertLevel.Low,
                "${demoCourseNames[index]}：请关注学分完成情况（演示）", "还需 ${index % 3 + 1} 学分") }
            return AcademicAlert(USER_ID, pageIndex, 3, items.drop((pageIndex - 1) * 10).take(10))
        }

        override suspend fun getAcademicProgram() = AcademicProgram(USER_ID, """
            <html><body><h1>计算机科学与技术专业培养方案（演示）</h1>
            <p>本页面内容为虚构数据，仅用于演示。</p><h2>培养目标</h2>
            <p>掌握计算机科学基础知识，具备软件设计、开发与实践能力。</p>
            <h2>学制与学分</h2><p>学制四年，毕业应修 160 学分。</p>
            <table border="1"><tr><th>课程模块</th><th>应修学分</th></tr>
            <tr><td>通识教育</td><td>40</td></tr><tr><td>专业课程</td><td>90</td></tr>
            <tr><td>实践教学</td><td>30</td></tr></table>
            ${demoCourseNames.chunked(5).mapIndexed { index, names -> "<h2>第 ${index + 1} 组课程与学习要求</h2><p>课程：${names.joinToString("、")}。</p><p>学生应完成理论学习、课堂讨论及实践任务，并通过课程考核。具体教学与毕业要求以正式培养方案为准。</p>" }.joinToString("\n")}
            </body></html>
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
        ) + CourseSelectCategory.entries.flatMap { category ->
            demoCourseNames.take(48).mapIndexed { index, name ->
                val room = "${if (index % 2 == 0) "教学楼" else "实验楼"} ${('A'.code + index % 3).toChar()}${201 + index}"
                val full = index % 13 == 8
                selectable("demo-${category.name}-$index", name, teachers[index % teachers.size].name,
                    DayOfWeek.entries[index % 5], index % 6 * 2 + 1, category).copy(
                    classroom = room, campusId = if (index % 3 == 0) "1" else "2",
                    schedules = listOf(SelectableCourse.ClassSchedule((1..16).map(Int::toString), "1-16", index % 5 + 1,
                        "${index % 6 * 2 + 1}-${index % 6 * 2 + 2}", room)),
                    credits = if (category == CourseSelectCategory.General) 1.0 + index % 3 else 2.0 + index % 3,
                    totalHours = 16 * (1 + index % 4), department = demoDepartments[index % demoDepartments.size],
                    remaining = if (full) 0 else 2 + index % 22, enrolled = if (full) 40 else 38 - index % 22,
                    conflictDescription = if (index % 11 == 7) "与已选课程的上课时间冲突" else "",
                    isOpenForSelection = index % 17 != 12,
                    note = if (index % 9 == 0) "包含课堂展示与小组实践，请留意课程安排。" else null,
                )
            }
        }
        private val selectedIds = (listOf("demo-select-1") + listOf(
            CourseSelectCategory.General to 9, CourseSelectCategory.Professional to 6, CourseSelectCategory.Interprofessional to 2,
        ).flatMap { (category, count) -> catalog.filter {
            val first = when (category) { CourseSelectCategory.Professional -> 20; CourseSelectCategory.Interprofessional -> 36; else -> 0 }
            it.id.startsWith("demo-${category.name}-") && (it.id.substringAfterLast('-').toIntOrNull() ?: -1) >= first &&
                it.remaining > 0 && it.isOpenForSelection && it.conflictDescription.isBlank()
        }.take(count).map { it.id } }).toMutableSet()
        private val dropLogs = (0 until 30).map { index ->
            val course = catalog[20 + index]
            CourseDropLog(course.courseId, course.name, course.credits, "选修", course.teacher, listOf(course.schedule),
                course.selectionCategory.orEmpty(), "学生退课", Clock.System.now().minus(kotlin.time.Duration.parse("${index / 3 + 1}d") + kotlin.time.Duration.parse("${index % 3}h"))
                    .toLocalDateTime(TimeZone.currentSystemDefault()).toString().replace('T', ' '), USER_ID,
                listOf("调整课程安排", "更换上课时段", "调整学习计划")[index % 3])
        }.toMutableList()

        private val creditGroups = listOf("本轮总学分", "通识选修", "专业计划", "跨学科选修", "通识核心", "专业方向", "文化与艺术", "实践与创新", "语言与交流", "体育与健康")

        override suspend fun getEntrances() = listOf(CourseSystemEntrance(
            "demo-entrance", USER_ID, semester, "${semester} 演示选课",
            LocalDateTime(semester.year, 1, 1, 0, 0), LocalDateTime(semester.year + 1, 12, 31, 23, 59),
        ))

        override suspend fun enterCourseSystem(id: String) {
            require(id == "demo-entrance") { "演示选课批次不存在。" }
        }

        override suspend fun getCreditSummary() = mutex.withLock {
            val selected = catalog.filter { it.id in selectedIds }
            CreditSummary("演示选退课仅影响虚构数据，退出应用后重置。", creditGroups.mapIndexed { index, name ->
                val credits = when (index) {
                    0 -> selected.sumOf { it.credits }
                    1 -> selected.filter { it.selectionCategoryCode == CourseSelectCategory.General.name }.sumOf { it.credits }
                    2 -> selected.filter { it.selectionCategoryCode == CourseSelectCategory.Professional.name }.sumOf { it.credits }
                    3 -> selected.filter { it.selectionCategoryCode in listOf(CourseSelectCategory.CrossGrade.name, CourseSelectCategory.Interprofessional.name) }.sumOf { it.credits }
                    4 -> selected.filter { it.id in (0..4).map { "demo-General-$it" } }.sumOf { it.credits }
                    5 -> selected.filter { it.id in (20..25).map { "demo-Professional-$it" } }.sumOf { it.credits }
                    6 -> selected.filter { course -> listOf("艺术", "音乐", "文化", "摄影").any { it in course.name } }.sumOf { it.credits }
                    7 -> selected.filter { "实践" in it.name || "创新" in it.name }.sumOf { it.credits }
                    8 -> selected.filter { "英语" in it.name || "交流" in it.name }.sumOf { it.credits }
                    else -> selected.filter { "体育" in it.name || "健康" in it.name }.sumOf { it.credits }
                }
                CreditSummary.CreditProgress(name, credits.toString(), listOf("60", "42", "24", "12", "18", "16", "10", "8", "10", "8")[index])
            })
        }

        override suspend fun getSelectedCourses() = mutex.withLock {
            catalog.filter { it.id in selectedIds }.map {
                SelectedCourse(it.id, it.courseId, it.name, it.credits, "选修", it.teacher, it.schedule, it.classroom)
            }
        }

        override suspend fun getSelectedCourseTimetable() = mutex.withLock {
            SelectedCourseTimetable((1..12 step 2).map { start ->
                SelectedCourseTimetable.Row("$start-${start + 1}", (1..7).map { day ->
                    catalog.filter { it.id in selectedIds }.filter { course ->
                        course.schedules.any { it.dayOfWeek == day && Regex("\\d+").find(it.sections)?.value?.toIntOrNull() == start }
                    }.joinToString("\n\n") { "${it.name}\n${it.teacher}\n${it.classroom.orEmpty()}\n1-16周" }
                })
            })
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
                    (extra.teacher.trim() in course.teacher || extra.teacher.trim() in course.name) &&
                    (extra.dayOfWeek == null || course.schedules.any { it.dayOfWeek == extra.dayOfWeek?.ordinal?.plus(1) }) &&
                    (extra.section == null || course.schedules.any { it.sections == extra.section.toString() }) &&
                    (!extra.filterFull || !course.isFull) &&
                    (!extra.filterConflicts || course.conflictDescription.isBlank()) &&
                    (params !is CourseSearchParam.General || params.campus == null || course.campusId == if (params.campus == Campus.Foshan) "1" else "2")
                )
            }
            CourseSearchResponse(filtered.drop(params.pageIndex * params.pageSize).take(params.pageSize),
                null, available.size, filtered.size)
        }

        override suspend fun selectCourse(id: String, category: CourseSelectCategory, priority: CourseSelectPriority?) = mutex.withLock {
            val course = catalog.firstOrNull { it.id == id && it.selectionCategoryCode == category.name }
                ?: return@withLock CourseOperateResult(false, "演示课程不存在或分类不匹配。")
            if (!course.isOpenForSelection || course.isDeleted || course.remaining <= 0) return@withLock CourseOperateResult(false, "课程未开放或名额已满。")
            if (course.conflictDescription.isNotBlank()) return@withLock CourseOperateResult(false, course.conflictDescription)
            if (!selectedIds.add(course.id)) CourseOperateResult(false, "已选过这门课程。")
            else CourseOperateResult(true, "演示选课成功。")
        }

        override suspend fun dropCourse(id: String) = mutex.withLock {
            val course = catalog.firstOrNull { it.id == id }
            if (course == null || !selectedIds.remove(id)) return@withLock CourseOperateResult(false, "尚未选择这门课程。")
            dropLogs.add(0, CourseDropLog(course.courseId, course.name, course.credits, "选修", course.teacher,
                listOf(course.schedule), "演示课程", "学生退课", Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString().replace('T', ' '), USER_ID, "演示操作"))
            CourseOperateResult(true, "演示退课成功。")
        }
    }

    private fun selectable(id: String, name: String, teacher: String, day: DayOfWeek, firstSection: Int, category: CourseSelectCategory) = SelectableCourse(
        id = id, courseId = "DEMO-$id", name = name, credits = 2.0, totalHours = 32,
        teacher = teacher, teacherIds = teachers.first { it.name == teacher }.id, schedule = "1-16周 星期${"一二三四五六日"[day.ordinal]} $firstSection-${firstSection + 1}节",
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
