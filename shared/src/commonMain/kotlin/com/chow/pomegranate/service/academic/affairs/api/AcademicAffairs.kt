package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.internal.AcademicAffairsImpl
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
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
    /** 认证 */
    val auth: Auth

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
}