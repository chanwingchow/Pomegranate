package com.chow.pomegranate.session

import com.chow.pomegranate.data.local.ExtracurricularSessionDao
import com.chow.pomegranate.data.local.UserDao
import com.chow.pomegranate.data.local.UserEntity
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.decodeCookies
import com.chow.pomegranate.service.academic.affairs.internal.encodeCookies
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.tool.ocr.OcrEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * 会话管理器实现。
 *
 * @param auth 教务系统认证模块
 * @param extracurricularActivity 第二课堂
 * @param sessionApiClient 原始 Http 客户端，绕过会话拦截，用于登出与会话校验，避免递归重登
 * @param cookieStorage Cookie 仓库
 * @param userDao 用户 DAO
 * @param ocrEngine 验证码识别引擎
 * @param secureStore 平台安全存储
 */
@Single
class SessionManagerImpl(
    private val auth: AcademicAffairs.Auth,
    private val extracurricularActivity: ExtracurricularActivity,
    private val sessionApiClient: SessionApiClient,
    private val cookieStorage: CookieStorage,
    private val userDao: UserDao,
    private val extracurricularSessionDao: ExtracurricularSessionDao,
    private val ocrEngine: OcrEngine,
    private val secureStore: SecureStore,
) : SessionManager {
    /** 防止并发登录。 */
    private val mutex = Mutex()

    /** 账号列表使用的协程作用域。 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val userId: MutableStateFlow<String?>
        field = MutableStateFlow<String?>(null)

    override val state: MutableStateFlow<SessionState>
        field = MutableStateFlow<SessionState>(SessionState.Inactive)

    override val accounts: StateFlow<List<Account>> = userDao.observeAll()
        .map { users -> users.map { Account(it.userId, it.isActive) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun getCaptcha(): ByteArray = auth.getCaptcha()

    override suspend fun login(
        userId: String,
        password: String,
        secondClassroomPassword: String,
        captcha: String,
    ) {
        val saved = userDao.user(userId)

        // 未填写密码时复用本地已保存的密码
        val resolvedPassword = password.ifBlank { saved?.let { secureStore.get(it.userId) }.orEmpty() }
        require(resolvedPassword.isNotBlank()) { "请输入密码。" }

        // 第二课堂密码缺省使用学号，与已保存账号的处理保持一致；保证写入安全存储的永远是非空值
        val resolvedSecondClassroomPassword = secondClassroomPassword
            .ifBlank { secondClassroomPasswordFor(saved) }
            .ifBlank { userId }

        mutex.withLock {
            establish(
                userId = userId,
                password = resolvedPassword,
                secondClassroomPassword = resolvedSecondClassroomPassword,
                savedCookies = cookiesFor(saved),
                captcha = captcha,
            )

            persist(
                userId = userId,
                password = resolvedPassword,
                secondClassroomPassword = resolvedSecondClassroomPassword,
            )
        }
    }

    override suspend fun autoLogin() {
        val user = userDao.activeUser() ?: return

        mutex.withLock {
            // 已经登录同一个账号时不重复登录
            if (state.value == SessionState.Active && userId.value == user.userId) return@withLock

            // 先尝试复用已保存的 Cookie，避免每次启动都走验证码登录
            if (restoreAndValidate(user)) return@withLock

            val password = secureStore.get(user.userId)

            // 没有本地密码时无法自动登录，保留未登录状态
            if (password.isNullOrBlank()) {
                state.value = SessionState.Inactive
                return@withLock
            }

            runCatching {
                establish(
                    userId = user.userId,
                    password = password,
                    secondClassroomPassword = resolvedSecondClassroomPassword(user),
                    savedCookies = cookiesFor(user),
                )
            }.onFailure {
                state.value = SessionState.Inactive
            }
        }
    }

    override suspend fun switchAccount(userId: String) {
        mutex.withLock {
            // 已经登录同一个账号时不重复处理
            if (this.userId.value == userId && state.value == SessionState.Active) return@withLock

            val user = userDao.user(userId) ?: error("账号不存在。")

            // 优先复用已保存的 Cookie
            if (restoreAndValidate(user)) return@withLock

            // Cookie 已失效，使用本地凭据自动重新登录
            val password = secureStore.get(user.userId)

            if (password.isNullOrBlank()) {
                state.value = SessionState.Expired
                throw SessionExpiredException()
            }

            runCatching {
                establish(
                    userId = user.userId,
                    password = password,
                    secondClassroomPassword = resolvedSecondClassroomPassword(user),
                    savedCookies = cookiesFor(user),
                )
            }.onFailure {
                state.value = SessionState.Expired
                throw SessionExpiredException()
            }
        }
    }

    override suspend fun deleteAccount(userId: String) {
        val wasActive = userDao.user(userId)?.isActive == true

        userDao.remove(userId)
        secureStore.remove(userId)
        secureStore.remove(cookieKey(userId))
        secureStore.remove(secondClassroomPasswordKey(userId))
        extracurricularSessionDao.remove(userId)

        if (!wasActive) return

        // 当前账号被删除，回退到其他账号
        cookieStorage.clear()
        this.userId.value = null
        state.value = SessionState.Inactive

        userDao.users().firstOrNull()?.let { next ->
            runCatching { switchAccountInternal(next) }
        }
    }

    override suspend fun refresh() {
        mutex.withLock {
            val user = userDao.activeUser() ?: throw SessionExpiredException()
            val password = secureStore.get(user.userId)

            if (password.isNullOrBlank()) {
                state.value = SessionState.Expired
                throw SessionExpiredException()
            }

            runCatching {
                establish(
                    userId = user.userId,
                    password = password,
                    secondClassroomPassword = resolvedSecondClassroomPassword(user),
                    savedCookies = cookiesFor(user),
                )
            }.onFailure {
                state.value = SessionState.Expired
                throw SessionExpiredException()
            }
        }
    }

    override suspend fun logout() {
        val loggedOutUserId = userId.value

        // 服务端登出失败不应阻塞本地登出
        runCatching { auth.logout() }

        cookieStorage.clear()
        userId.value = null
        state.value = SessionState.Inactive

        // 同时清掉持久化的 Cookie：只清内存罐的话，下次启动会拿旧 Cookie
        // 直接恢复成已登录，公共设备上等于没登出。
        if (loggedOutUserId != null) {
            secureStore.remove(cookieKey(loggedOutUserId))
        }
    }

    override suspend fun awaitLogin(): Boolean? {
        // 已经登录，无需等待
        if (state.value == SessionState.Active) return false

        // 没有任何可自动登录的账号，直接返回，由调用方提示手动登录
        val user = userDao.activeUser() ?: return null
        if (secureStore.get(user.userId).isNullOrBlank()) return null

        state.first { it == SessionState.Active }

        return true
    }

    /**
     * 执行一次完整的登录流程，成功后更新登录态。
     *
     * @param userId 学号
     * @param password 教务系统密码
     * @param secondClassroomPassword 第二课堂密码
     * @param savedCookies 本地已保存的 Cookie，登录前先恢复以复用服务端会话
     * @param captcha 手动填写的验证码；留空时由程序自动识别
     */
    private suspend fun establish(
        userId: String,
        password: String,
        secondClassroomPassword: String,
        savedCookies: String,
        captcha: String = "",
    ) {
        // 恢复 Cookie
        cookieStorage.restore(decodeCookies(savedCookies))

        // 用户手填的验证码不重试，自动识别的允许重试一次
        attemptLogin(
            userId = userId,
            password = password,
            retryOnCaptchaError = captcha.isBlank(),
            captcha = captcha,
        )

        userDao.setActive(userId)

        this.userId.value = userId
        state.value = SessionState.Active

        // 第二课堂登录失败不影响教务系统登录
        runCatching {
            loginSecondClassroom(userId, secondClassroomPassword)
        }
    }

    /**
     * 提交一次登录请求。
     *
     * @param retryOnCaptchaError 验证码识别错误时是否重新识别并重试
     * @param captcha 手动填写的验证码；留空时由程序自动识别
     */
    private suspend fun attemptLogin(
        userId: String,
        password: String,
        retryOnCaptchaError: Boolean,
        captcha: String = "",
    ) {
        val resolvedCaptcha = resolveCaptcha(captcha)

        when (val result = auth.login(LoginParam(userId, password, resolvedCaptcha))) {
            is LoginResult.Success -> Unit

            is LoginResult.Fail.CaptchaError -> {
                if (retryOnCaptchaError) {
                    attemptLogin(userId, password, retryOnCaptchaError = false)
                } else {
                    throw LoginException("验证码错误，请重试。")
                }
            }

            is LoginResult.Fail.Unknow -> throw LoginException(
                result.throwable.message ?: "登录失败，请稍后重试。",
            )
        }
    }

    /**
     * 返回本次登录使用的验证码。
     *
     * @param captcha 用户手动填写的验证码，为空时从服务端获取并自动识别
     */
    private suspend fun resolveCaptcha(captcha: String): String {
        if (captcha.isNotBlank()) return captcha.filter { it.isLetterOrDigit() }

        check(ocrEngine.isAvailable()) { "当前平台不支持验证码识别，请手动填写验证码。" }

        val image = auth.getCaptcha()

        // 验证码只包含数字与字母，过滤掉识别噪声
        return ocrEngine.recognize(image).filter { it.isLetterOrDigit() }
    }

    /** 持久化账号信息。 */
    private suspend fun persist(
        userId: String,
        password: String,
        secondClassroomPassword: String,
    ) {
        secureStore.put(userId, password)
        if (secondClassroomPassword.isNotBlank()) {
            secureStore.put(secondClassroomPasswordKey(userId), secondClassroomPassword)
        }
        secureStore.put(cookieKey(userId), encodeCookies(cookieStorage.snapshot()))

        val saved = userDao.user(userId)

        userDao.save(
            UserEntity(
                userId = userId,
                secondClassroomPassword = "",
                cookies = "",
                isActive = true,
                savedAt = saved?.savedAt ?: Clock.System.now().toEpochMilliseconds(),
            ),
        )

        // 保存 Cookie 之后再次确认当前账号唯一
        userDao.setActive(userId)
    }

    /**
     * 复用 [user] 已保存的 Cookie 并校验登录状态。
     *
     * @return 校验通过返回 `true`
     */
    private suspend fun restoreAndValidate(user: UserEntity): Boolean {
        cookieStorage.restore(decodeCookies(cookiesFor(user)))

        this.userId.value = user.userId

        val isValid = runCatching { validate() }
            .getOrElse { false }

        if (!isValid) {
            this.userId.value = null
            return false
        }

        userDao.setActive(user.userId)
        state.value = SessionState.Active

        // 复用 Cookie 后同步第二课堂登录态
        runCatching {
            loginSecondClassroom(user.userId, resolvedSecondClassroomPassword(user))
        }

        return true
    }

    /**
     * 登录第二课堂并保存会话。
     *
     * 第二课堂的接口依赖登录返回的会话 ID 与 `X-token`，重启后无法从 Cookie 恢复，
     * 因此每次应用启动或切换账号都会重新登录一次。
     */
    private suspend fun loginSecondClassroom(
        userId: String,
        password: String,
    ) {
        val sessionId = extracurricularActivity.auth.login(userId, password)

        // 会话 ID 只在本次进程内由客户端使用，重启后仍须重新登录；不落盘。
        extracurricularSessionDao.remove(userId)
    }

    /**
     * 校验当前 Cookie 是否仍然有效。
     *
     * 通过原始客户端访问课表接口：未登录时服务端会重定向到登录页，
     * 由 [isLoginPage] 识别后即视为失效。
     */
    private suspend fun validate(): Boolean = runCatching {
        val response = sessionApiClient.client.get("/jsxsd/xskb/xskb_list.do") {
            parameter("xnxq01id", Semester.now())
        }

        !response.isLoginPage()
    }.getOrDefault(false)

    /** 切换账号，供删除账号后回退使用。 */
    private suspend fun switchAccountInternal(user: UserEntity) {
        if (restoreAndValidate(user)) return

        val password = secureStore.get(user.userId) ?: return

        runCatching {
            establish(
                userId = user.userId,
                password = password,
                secondClassroomPassword = resolvedSecondClassroomPassword(user),
                savedCookies = cookiesFor(user),
            )
        }
    }

    /** 返回账号的第二课堂密码，缺省为学号，并迁移旧版 Room 字段。 */
    private suspend fun resolvedSecondClassroomPassword(user: UserEntity): String = secondClassroomPasswordFor(user)

    private suspend fun secondClassroomPasswordFor(user: UserEntity?): String {
        val userId = user?.userId ?: return ""

        // 空串不是有效密码：否则缺省逻辑（默认使用学号）会被一条空记录挡住
        secureStore.get(secondClassroomPasswordKey(userId))
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val password = user.secondClassroomPassword.ifBlank { userId }
        secureStore.put(secondClassroomPasswordKey(userId), password)
        if (user.secondClassroomPassword.isNotBlank()) {
            userDao.save(user.copy(secondClassroomPassword = ""))
        }
        return password
    }

    /** 从安全存储读取 Cookie，并在首次访问时迁移旧版 Room 数据。 */
    private suspend fun cookiesFor(user: UserEntity?): String {
        val userId = user?.userId ?: return ""
        secureStore.get(cookieKey(userId))?.let { return it }
        if (user.cookies.isNotBlank()) {
            secureStore.put(cookieKey(userId), user.cookies)
            userDao.save(user.copy(cookies = ""))
        }
        return user.cookies
    }

    private companion object {
        fun cookieKey(userId: String) = "cookies:$userId"

        fun secondClassroomPasswordKey(userId: String) = "second-classroom-password:$userId"
    }
}
