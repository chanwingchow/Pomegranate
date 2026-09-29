package com.chow.pomegranate.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.Res
import com.chow.pomegranate.captcha
import com.chow.pomegranate.login_failed
import com.chow.pomegranate.password_required
import com.chow.pomegranate.username_required
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.utils.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel

/** 登录页 ViewModel。 */
@KoinViewModel
class LoginViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    /** 是否正在登录。 */
    val isLoggingIn: MutableStateFlow<Boolean>
        field = MutableStateFlow(false)

    /** 登录失败信息，为空表示没有错误。 */
    val errorMessage: MutableStateFlow<StringResource?>
        field = MutableStateFlow<StringResource?>(null)

    /** 登录失败的原始原因，用于提示用户。 */
    val errorDetail: MutableStateFlow<String?>
        field = MutableStateFlow<String?>(null)

    /** 验证码图片，为空表示未获取。 */
    val captcha: MutableStateFlow<ByteArray?>
        field = MutableStateFlow<ByteArray?>(null)

    /** 用户手动填写的验证码；留空时由程序自动识别。 */
    val captchaText: MutableStateFlow<String>
        field = MutableStateFlow("")

    /** 是否正在获取验证码。 */
    val isLoadingCaptcha: MutableStateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        refreshCaptcha()
    }

    /** 刷新验证码。 */
    fun refreshCaptcha() {
        if (isLoadingCaptcha.value) return

        viewModelScope.launch {
            isLoadingCaptcha.value = true
            captchaText.value = ""

            runSuspendCatching { sessionManager.getCaptcha() }
                .onSuccess { captcha.value = it }
                .onFailure { throwable ->
                    errorMessage.value = Res.string.captcha
                    errorDetail.value = throwable.message
                }

            isLoadingCaptcha.value = false
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
        onSuccess: () -> Unit,
    ) {
        if (isLoggingIn.value) return

        errorMessage.value = null
        errorDetail.value = null

        when {
            userId.isBlank() -> {
                errorMessage.value = Res.string.username_required
                return
            }

            password.isBlank() -> {
                errorMessage.value = Res.string.password_required
                return
            }
        }

        viewModelScope.launch {
            isLoggingIn.value = true

            try {
                runSuspendCatching {
                    sessionManager.login(
                        userId = userId,
                        password = password,
                        secondClassroomPassword = secondClassroomPassword,
                        captcha = captcha,
                    )
                }.onSuccess {
                    onSuccess()
                }.onFailure { throwable ->
                    errorMessage.value = Res.string.login_failed
                    errorDetail.value = throwable.message

                    // 登录失败后验证码已失效
                    refreshCaptcha()
                }
            } finally {
                // 必须放在 finally：onSuccess 里的导航回调一旦抛异常，
                // 否则 isLoggingIn 会永远停在 true，登录按钮永久禁用。
                isLoggingIn.value = false
            }
        }
    }
}
