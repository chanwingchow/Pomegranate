package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.internal.AcademicAffairsImpl
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest

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
         */
        suspend fun getTimetable(semester: Semester): Timetable
    }
}