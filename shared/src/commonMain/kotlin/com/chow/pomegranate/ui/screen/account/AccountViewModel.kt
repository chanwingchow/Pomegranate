package com.chow.pomegranate.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.session.SessionManager
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * 账号管理弹窗 ViewModel。
 *
 * 登出与切号必须放在这里，而不是弹窗的 `rememberCoroutineScope`：这两个动作的结果
 * 都是"关掉弹窗"，弹窗一离开组合，它的 scope 就被取消——服务端可能已经登出，
 * 本地却停在登录态；切号同理会被打断。
 */
@KoinViewModel
class AccountViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    /**
     * 退出登录。
     *
     * @param onResult 完成回调，参数为失败原因；`null` 表示成功
     */
    fun logout(onResult: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            val failure = runCatching { sessionManager.logout() }.exceptionOrNull()

            onResult(failure)
        }
    }

    /**
     * 切换到 [userId] 账号。
     *
     * @param onResult 完成回调，参数为失败原因；`null` 表示成功
     */
    fun switchAccount(
        userId: String,
        onResult: (Throwable?) -> Unit = {},
    ) {
        viewModelScope.launch {
            val failure = runCatching { sessionManager.switchAccount(userId) }.exceptionOrNull()

            onResult(failure)
        }
    }

    /**
     * 删除账号。
     *
     * @param onResult 完成回调，参数为失败原因；`null` 表示成功
     */
    fun deleteAccount(
        userId: String,
        onResult: (Throwable?) -> Unit = {},
    ) {
        viewModelScope.launch {
            val failure = runCatching { sessionManager.deleteAccount(userId) }.exceptionOrNull()

            onResult(failure)
        }
    }
}
