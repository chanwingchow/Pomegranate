package com.chow.pomegranate.session

import kotlinx.coroutines.flow.StateFlow

/**
 * 登录状态。
 */
sealed interface SessionState {
    /** 未登录。 */
    data object Inactive : SessionState

    /** 已登录。 */
    data object Active : SessionState

    /** 登录状态已过期，且自动重新登录失败。 */
    data object Expired : SessionState
}

/**
 * 已保存的账号。
 *
 * @property userId 学号
 * @property isActive 是否为当前登录账号
 */
data class Account(
    val userId: String,
    val isActive: Boolean,
)

/**
 * 会话管理器。
 *
 * 统一持有登录态与本地凭据，负责：
 * - 首次登录、启动自动登录；
 * - 登录过期后的透明重新登录；
 * - 多账号的切换与删除；
 * - Cookie 的持久化与恢复。
 */
interface SessionManager {
    /** 当前登录用户，未登录时为 `null`。 */
    val userId: StateFlow<String?>

    /** 当前登录状态。 */
    val state: StateFlow<SessionState>

    /** 全部已保存账号，当前账号优先。 */
    val accounts: StateFlow<List<Account>>

    /** 返回验证码图片。 */
    suspend fun getCaptcha(): ByteArray

    /**
     * 登录并保存为账号。
     *
     * @param userId 学号
     * @param password 教务系统密码；传入空字符串时复用本地已保存的密码
     * @param secondClassroomPassword 第二课堂密码；传入空字符串时复用本地已保存的密码
     * @param captcha 手动填写的验证码；留空时由程序自动识别
     */
    suspend fun login(
        userId: String,
        password: String,
        secondClassroomPassword: String,
        captcha: String = "",
    )

    /** 使用当前账号的本地凭据自动登录；没有本地账号时不做任何事。 */
    suspend fun autoLogin()

    /**
     * 切换到 [userId]。
     *
     * 优先复用该账号已保存的 Cookie；校验失败时使用本地凭据自动重新登录。
     */
    suspend fun switchAccount(userId: String)

    /** 删除 [userId]；若删除的是当前账号，则自动切换到其他账号。 */
    suspend fun deleteAccount(userId: String)

    /**
     * 重新登录以恢复登录状态。
     *
     * 没有本地凭据、或自动登录失败时抛出 [SessionExpiredException]。
     */
    suspend fun refresh()

    /** 退出登录，保留本地账号以便下次自动登录。 */
    suspend fun logout()

    /**
     * 挂起直到登录成功。
     *
     * 用于需要登录的页面：启动时若尚未登录，应先在这里等待自动登录或用户手动登录，
     * 而不是立即请求受保护接口。
     *
     * 只有存在可自动登录的账号时才等待；没有任何已保存账号时立即返回 `null`，
     * 由调用方提示用户手动登录。
     *
     * @return 本次调用导致了登录返回 `true`；进入时已登录返回 `false`；无法自动登录返回 `null`
     */
    suspend fun awaitLogin(): Boolean?
}
