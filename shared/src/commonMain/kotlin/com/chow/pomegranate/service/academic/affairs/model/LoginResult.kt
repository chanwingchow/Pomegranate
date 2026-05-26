package com.chow.pomegranate.service.academic.affairs.model

/**
 * 登录结果。
 */
sealed interface LoginResult {
    /**
     * 登录成功。
     */
    data object Success: LoginResult

    /**
     * 登录失败。
     */
    sealed interface Fail: LoginResult {
        /**
         * 验证码错误。
         */
        data object CaptchaError: Fail

        /**
         * 未知错误。
         */
        data class Unknow(val throwable: Throwable): Fail
    }
}