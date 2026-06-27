package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.internal.AcademicAffairsImpl
import com.chow.pomegranate.service.academic.affairs.model.AcademicAlert
import com.chow.pomegranate.service.academic.affairs.model.AcademicPlan
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgram
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgress
import com.chow.pomegranate.service.academic.affairs.model.CampusCalendar
import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchResponse
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectPriority
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.academic.affairs.model.Exemption
import com.chow.pomegranate.service.academic.affairs.model.GraduationAudit
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import com.chow.pomegranate.service.academic.affairs.model.Teacher
import com.chow.pomegranate.service.academic.affairs.model.Teachers
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 教务系统。
 */
fun AcademicAffairs(): AcademicAffairs {
    return AcademicAffairsImpl(
        httpClient = HttpClient {
            // 默认请求
            defaultRequest {
                url("http://jwxt.gdufe.edu.cn")
            }
            // Cookie
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                // Json 解析
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json(json)
                json(
                    json,
                    // 教务系统返回的数据是 Json，但标记的类型是 HTML
                    contentType = ContentType.Text.Html,
                )
            }
        },
    )
}

/**
 * 教务系统。
 */
interface AcademicAffairs {
    /** 认证模块 */
    val auth: Auth

    /** 课程模块 */
    val enrollment: Enrollment

    /** 学业模块 */
    val academic: Academic

    /** 选课系统模块 */
    val courseSystem: CourseSystem

    /**
     * 认证模块。
     */
    interface Auth {
        /**
         * 返回验证码图片。
         */
        suspend fun getCaptcha(): ByteArray

        /**
         * 登录。
         *
         * @param param 登录参数
         */
        suspend fun login(param: LoginParam): LoginResult

        /**
         * 退出登录。
         */
        suspend fun logout()
    }

    /**
     * 修读模块。
     */
    interface Enrollment {
        /**
         * 返回课表。
         *
         * @param semester 学期
         */
        suspend fun getTimetable(semester: Semester): Timetable

        /**
         * 返回课程课表。
         *
         * @param semester 学期
         */
        suspend fun getCourseTimetable(semester: Semester): CourseTimetable

        /**
         * 返回考试安排。
         *
         * @param semester 学期
         */
        suspend fun getExamSchedule(semester: Semester): ExamSchedule

        /**
         * 返回课程成绩。
         */
        suspend fun getCourseTranscript(): CourseTranscript

        /**
         * 返回等级考试成绩。
         */
        suspend fun getLevelTranscript(): LevelTranscript

        /**
         * 返回免听申请。
         *
         * @param semester 学期
         */
        suspend fun getExemption(semester: Semester): Exemption

        /**
         * 返回校历。
         *
         * @param semester 学期
         */
        suspend fun getCampusCalendar(semester: Semester): CampusCalendar

        /**
         * 返回教师列表。
         */
        suspend fun getTeachers(query: String = "", pageIndex: Int = 1): Teachers

        /**
         * 返回教师。
         */
        suspend fun getTeacher(id: String): Teacher
    }

    /**
     * 学业模块。
     */
    interface Academic {
        /**
         * 返回学业计划。
         */
        suspend fun getAcademicPlan(): AcademicPlan

        /**
         * 返回学业进度。
         *
         * @param isPrimary 是否为主修
         */
        suspend fun getAcademicProgress(isPrimary: Boolean = true): AcademicProgress

        /**
         * 返回学业预警。
         */
        suspend fun getAcademicAlert(pageIndex: Int = 1): AcademicAlert

        /**
         * 返回培养方案。
         */
        suspend fun getAcademicProgram(): AcademicProgram

        /**
         * 返回毕业审核。
         */
        suspend fun getGraduationAudit(isPrimary: Boolean = true): GraduationAudit

        /**
         * 返回毕业审核报告。
         */
        suspend fun getGraduationAuditReport(urlString: String): ByteArray
    }

    /**
     * 选课系统模块。
     */
    interface CourseSystem {
        /**
         * 返回选课入口。
         */
        suspend fun getEntrances(): List<CourseSystemEntrance>

        /**
         * 进入选课系统。
         */
        suspend fun enterCourseSystem(id: String)

        /**
         * 返回选课学分概览。
         */
        suspend fun getCreditSummary(): CreditSummary

        /**
         * 返回已选课程。
         */
        suspend fun getSelectedCourses(): List<SelectedCourse>

        /**
         * 返回退课记录。
         */
        suspend fun getCourseDropLogs(): List<CourseDropLog>

        /**
         * 搜索课程。
         */
        suspend fun searchCourses(params: CourseSearchParam): CourseSearchResponse

        /**
         * 选课。
         *
         * @param id 选课操作 id
         */
        suspend fun selectCourse(
            id: String,
            category: CourseSelectCategory,
            priority: CourseSelectPriority? = null,
        )

        /**
         * 退课。
         *
         * @param id 选课操作 id
         */
        suspend fun dropCourse(id: String)
    }
}