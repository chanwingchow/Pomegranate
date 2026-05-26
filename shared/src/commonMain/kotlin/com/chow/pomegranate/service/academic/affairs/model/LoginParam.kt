package com.chow.pomegranate.service.academic.affairs.model

/**
 * 登录参数。
 *
 * @param username 学号
 * @param password 密码
 * @param captcha 验证码
 */
data class LoginParam(
    val username: String,
    val password: String,
    val captcha: String,
)