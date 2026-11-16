package com.chow.pomegranate.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.Res
import com.chow.pomegranate.captcha
import com.chow.pomegranate.login_failed
import com.chow.pomegranate.password_required
import com.chow.pomegranate.username_required
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.LoginRequest
import com.chow.pomegranate.session.LoginTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.chow.pomegranate.utils.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel

data class LoginError(val message: StringResource, val detail: String? = null)

/** 登录页 ViewModel。 */
@KoinViewModel
class LoginViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 是否正在登录。 */
    val isLoggingIn: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val errorEvents = Channel<LoginError>(Channel.BUFFERED)
    val errors = errorEvents.receiveAsFlow()

    /** 验证码图片，为空表示未获取。 */
    val captcha: StateFlow<ByteArray?>
        field = MutableStateFlow<ByteArray?>(null)

    /** 用户手动填写的验证码；留空时由程序自动识别。 */
    val captchaText: StateFlow<String>
        field = MutableStateFlow("")

    /** 是否正在获取验证码。 */
    val isLoadingCaptcha: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val accounts = sessionManager.accounts
    private var target = LoginTarget.Both
    private var captchaJob: Job? = null
    private var captchaVersion = 0

    fun selectTarget(target: LoginTarget) {
        this.target = target
        if (!target.academic) {
            captchaVersion++
            captchaJob?.cancel()
            isLoadingCaptcha.value = false
        } else if (captcha.value == null) refreshCaptcha()
    }

    /** 刷新验证码。 */
    fun refreshCaptcha() {
        if (!target.academic || isLoadingCaptcha.value) return
        isLoadingCaptcha.value = true
        val version = ++captchaVersion
        captchaJob = viewModelScope.launch {
            captcha.value = null
            captchaText.value = ""
            try {
                runSuspendCatching { sessionManager.getCaptcha() }
                    .onSuccess { if (version == captchaVersion) captcha.value = it }
                    .onFailure { throwable ->
                        if (version == captchaVersion) {
                            errorEvents.trySend(LoginError(Res.string.captcha, throwable.message))
                        }
                    }
            } finally { if (version == captchaVersion) isLoadingCaptcha.value = false }
        }
    }

    /**
     * 登录。
     *
     * 成功后回调 [onSuccess]。
     *
     * @param userId 学号
     * @param password 密码
     * @param secondClassroomPassword 第二课堂密码
     * @param captcha 手动填写的验证码，留空时自动识别
     */
    fun login(
        userId: String,
        password: String,
        secondClassroomPassword: String,
        captcha: String,
        target: LoginTarget = this.target,
        saveAccount: Boolean = true,
        onSuccess: () -> Unit,
    ) {
        if (isLoggingIn.value) return


        when {
            userId.isBlank() -> {
                errorEvents.trySend(LoginError(Res.string.username_required))
                return
            }

            target.academic && password.isBlank() && accounts.value.none { it.userId == userId.trim() } -> {
                errorEvents.trySend(LoginError(Res.string.password_required))
                return
            }
        }

        isLoggingIn.value = true
        viewModelScope.launch {
            try {
                runSuspendCatching {
                    sessionManager.signIn(LoginRequest(
                        userId = userId.trim(),
                        password = password,
                        secondClassroomPassword = secondClassroomPassword,
                        captcha = captcha,
                        target = target,
                        saveAccount = saveAccount,
                    ))
                }.onSuccess {
                    onSuccess()
                }.onFailure { throwable ->
                    errorEvents.trySend(LoginError(Res.string.login_failed, throwable.message))

                    // 登录失败后验证码已失效
                    if (target.academic) refreshCaptcha()
                }
            } finally {
                // 必须放在 finally：onSuccess 里的导航回调一旦抛异常，
                // 否则 isLoggingIn 会永远停在 true，登录按钮永久禁用。
                isLoggingIn.value = false
            }
        }
    }
}
