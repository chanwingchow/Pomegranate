package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.internal.AcademicAffairsImpl
import com.chow.pomegranate.service.academic.affairs.model.CourseDropLog
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.CourseSearchResponse
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectPriority
import com.chow.pomegranate.service.academic.affairs.model.CourseSystemEntrance
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.CreditSummary
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
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