package com.chow.pomegranate.session

import kotlinx.coroutines.withTimeoutOrNull

/** 等待自动登录的最长时间。 */
private const val LOGIN_WAIT_TIMEOUT_MILLIS = 10_000L

/**
 * 返回是否已登录，必要时等待自动登录完成。
 *
 * 需要登录的页面在加载前调用它：
 * - 已登录立即返回 `true`；
 * - 有可自动登录的账号则最多等待 [LOGIN_WAIT_TIMEOUT_MILLIS]，成功返回 `true`；
 * - 没有任何账号、或等待超时，返回 `false`，由页面提示手动登录。
 *
 * 超时上限可以避免用户在自动登录卡住时一直看到加载中。
 */
suspend fun SessionManager.isLoggedInWithWait(): Boolean {
    val result = withTimeoutOrNull(LOGIN_WAIT_TIMEOUT_MILLIS) { awaitLogin() }

    // null 表示无账号可登录或等待超时，false 表示进入时已登录
    return when (result) {
        true -> true
        false -> true
        null -> false
    }
}
