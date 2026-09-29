package com.chow.pomegranate.session

/**
 * 会话已过期。
 *
 * 当服务端判定登录状态失效，且使用本地凭据自动重新登录失败时抛出。
 * UI 层捕获该异常后应引导用户重新登录。
 */
class SessionExpiredException : IllegalStateException("登录状态已过期，请重新登录。")

/**
 * 登录失败。
 *
 * @param message 失败原因
 */
class LoginException(message: String) : IllegalStateException(message)
