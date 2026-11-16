package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.service.foundation.Semester

/** 统一路由；即使仓库保存了某个模块，切换后也会使用正确的服务。 */
class SwitchingAcademicAffairs(
    private val real: AcademicAffairs,
    private val demo: AcademicAffairs,
    private val mode: DemoModeRepository,
) : AcademicAffairs by RoutedAcademicAffairs({ if (mode.isEnabled()) demo else real })

/** 按请求选择实现，也供模拟服务统一添加可取消的响应延时。 */
internal class RoutedAcademicAffairs(private val backend: suspend (String) -> AcademicAffairs) : AcademicAffairs {

    override val auth = object : AcademicAffairs.Auth {
        override suspend fun getCaptcha(): ByteArray =
            backend("auth.getCaptcha").auth.getCaptcha()

        override suspend fun login(param: LoginParam): LoginResult =
            backend("auth.login").auth.login(param)

        override suspend fun logout() =
            backend("auth.logout").auth.logout()
    }

    override val enrollment = object : AcademicAffairs.Enrollment {
        override suspend fun getTimetable(semester: Semester): Timetable =
            backend("enrollment.getTimetable").enrollment.getTimetable(semester)

        override suspend fun getCourseTimetable(semester: Semester): CourseTimetable =
            backend("enrollment.getCourseTimetable").enrollment.getCourseTimetable(semester)

        override suspend fun getCourseTimetableSemesters(): List<Semester> =
            backend("enrollment.getCourseTimetableSemesters").enrollment.getCourseTimetableSemesters()

        override suspend fun getExamSchedule(semester: Semester): ExamSchedule =
            backend("enrollment.getExamSchedule").enrollment.getExamSchedule(semester)

        override suspend fun getCourseTranscript(): CourseTranscript =
            backend("enrollment.getCourseTranscript").enrollment.getCourseTranscript()

        override suspend fun getLevelTranscript(): LevelTranscript =
            backend("enrollment.getLevelTranscript").enrollment.getLevelTranscript()

        override suspend fun getExemption(semester: Semester): Exemption =
            backend("enrollment.getExemption").enrollment.getExemption(semester)

        override suspend fun getCampusCalendar(semester: Semester): CampusCalendar =
            backend("enrollment.getCampusCalendar").enrollment.getCampusCalendar(semester)

        override suspend fun getTeachers(query: String, pageIndex: Int): Teachers =
            backend("enrollment.getTeachers").enrollment.getTeachers(query, pageIndex)

        override suspend fun getTeacher(id: String): Teacher =
            backend("enrollment.getTeacher").enrollment.getTeacher(id)
    }

    override val academic = object : AcademicAffairs.Academic {
        override suspend fun getAcademicPlan(): AcademicPlan =
            backend("academic.getAcademicPlan").academic.getAcademicPlan()

        override suspend fun getAcademicProgress(isPrimary: Boolean): AcademicProgress =
            backend("academic.getAcademicProgress").academic.getAcademicProgress(isPrimary)

        override suspend fun getAcademicAlert(pageIndex: Int): AcademicAlert =
            backend("academic.getAcademicAlert").academic.getAcademicAlert(pageIndex)

        override suspend fun getAcademicProgram(): AcademicProgram =
            backend("academic.getAcademicProgram").academic.getAcademicProgram()

        override suspend fun getGraduationAudit(isPrimary: Boolean): GraduationAudit =
            backend("academic.getGraduationAudit").academic.getGraduationAudit(isPrimary)

        override suspend fun getGraduationAuditReport(urlString: String): ByteArray =
            backend("academic.getGraduationAuditReport").academic.getGraduationAuditReport(urlString)
    }

    override val courseSystem = object : AcademicAffairs.CourseSystem {
        override suspend fun getEntrances(): List<CourseSystemEntrance> =
            backend("courseSystem.getEntrances").courseSystem.getEntrances()

        override suspend fun enterCourseSystem(id: String) =
            backend("courseSystem.enterCourseSystem").courseSystem.enterCourseSystem(id)

        override suspend fun getCreditSummary(): CreditSummary =
            backend("courseSystem.getCreditSummary").courseSystem.getCreditSummary()

        override suspend fun getSelectedCourses(): List<SelectedCourse> =
            backend("courseSystem.getSelectedCourses").courseSystem.getSelectedCourses()

        override suspend fun getSelectedCourseTimetable(): SelectedCourseTimetable =
            backend("courseSystem.getSelectedCourseTimetable").courseSystem.getSelectedCourseTimetable()

        override suspend fun getCourseDropLogs(): List<CourseDropLog> =
            backend("courseSystem.getCourseDropLogs").courseSystem.getCourseDropLogs()

        override suspend fun searchCourses(params: CourseSearchParam): CourseSearchResponse =
            backend("courseSystem.searchCourses").courseSystem.searchCourses(params)

        override suspend fun selectCourse(id: String, category: CourseSelectCategory, priority: CourseSelectPriority?): CourseOperateResult =
            backend("courseSystem.selectCourse").courseSystem.selectCourse(id, category, priority)

        override suspend fun dropCourse(id: String): CourseOperateResult =
            backend("courseSystem.dropCourse").courseSystem.dropCourse(id)
    }
}
