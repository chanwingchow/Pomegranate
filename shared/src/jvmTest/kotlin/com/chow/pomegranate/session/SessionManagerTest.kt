package com.chow.pomegranate.session

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.BasicEvent
import com.chow.pomegranate.service.extracurricular.model.Event
import com.chow.pomegranate.service.extracurricular.model.EventState
import com.chow.pomegranate.service.extracurricular.model.MyEvent
import com.chow.pomegranate.service.extracurricular.model.Transcript
import com.chow.pomegranate.service.extracurricular.model.TranscriptLogs
import com.chow.pomegranate.tool.ocr.OcrEngine
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 会话管理器测试。
 *
 * 覆盖登录、验证码重试、凭据持久化与多账号切换。
 */
class SessionManagerTest {
    /** 登录成功后保存账号与密码。 */
    @Test
    fun loginPersistsAccountAndCredentials() = runBlocking<Unit> {
        val database = createDatabase()

        try {
            val userDao = database.userDao()
            val secureStore = FakeSecureStore()
            val auth = FakeAuth()

            val manager = createManager(database, auth, secureStore)

            manager.login("202400000001", "password", "second")

            val saved = userDao.user("202400000001")
            assertTrue(saved?.isActive == true)
            assertEquals("password", secureStore.get("202400000001"))

            // 第二课堂密码存在加密存储里；数据库列只服务于旧版迁移，登录后会被清空
            assertEquals(
                "second",
                secureStore.get("second-classroom-password:202400000001"),
            )
            assertEquals("", saved.secondClassroomPassword)
            assertEquals(SessionState.Active, manager.state.value)
            assertEquals("202400000001", manager.userId.value)
            assertEquals(1, auth.loginCount)
        } finally {
            database.close()
        }
    }

    /** 验证码错误时重新识别并重试。 */
    @Test
    fun retryOnceWhenCaptchaIsWrong() = runBlocking<Unit> {
        val database = createDatabase()

        try {
            val auth = FakeAuth(results = mutableListOf(LoginResult.Fail.CaptchaError, LoginResult.Success))
            val manager = createManager(database, auth)

            manager.login("202400000002", "password", "")

            assertEquals(2, auth.captchaCount)
            assertEquals(SessionState.Active, manager.state.value)
        } finally {
            database.close()
        }
    }

    /** 连续两次验证码错误时抛出登录异常。 */
    @Test
    fun failWhenCaptchaKeepsWrong() = runBlocking<Unit> {
        val database = createDatabase()

        try {
            val auth = FakeAuth(
                results = mutableListOf(LoginResult.Fail.CaptchaError, LoginResult.Fail.CaptchaError),
            )
            val manager = createManager(database, auth)

            assertFailsWith<LoginException> {
                manager.login("202400000003", "password", "")
            }
        } finally {
            database.close()
        }
    }

    /** 未填写密码且本地没有保存密码时拒绝登录。 */
    @Test
    fun rejectBlankPasswordWithoutSavedCredential() = runBlocking<Unit> {
        val database = createDatabase()

        try {
            val manager = createManager(database, FakeAuth())

            assertFailsWith<IllegalArgumentException> {
                manager.login("202400000004", "", "")
            }
        } finally {
            database.close()
        }
    }

    /** 账号列表按当前账号优先返回。 */
    @Test
    fun accountsMarkActiveUser() = runBlocking<Unit> {
        val database = createDatabase()

        try {
            val manager = createManager(database, FakeAuth())

            manager.login("202400000005", "password", "")
            manager.login("202400000006", "password", "")

            val accounts = manager.accounts.first { it.isNotEmpty() && it.any { account -> account.isActive } }

            assertEquals("202400000006", accounts.first { it.isActive }.userId)
        } finally {
            database.close()
        }
    }

    /** 创建内存数据库。 */
    private fun createDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    /** 创建被测的会话管理器。 */
    private fun createManager(
        database: AppDatabase,
        auth: AcademicAffairs.Auth,
        secureStore: SecureStore = FakeSecureStore(),
    ): SessionManager = SessionManagerImpl(
        auth = auth,
        extracurricularActivity = FakeExtracurricularActivity(),
        sessionApiClient = SessionApiClient(HttpClient(OkHttp)),
        cookieStorage = CookieStorage(),
        userDao = database.userDao(),
        extracurricularSessionDao = database.extracurricularSessionDao(),
        ocrEngine = FakeOcrEngine(),
        secureStore = secureStore,
    )
}

/** 假的验证码识别引擎。 */
private class FakeOcrEngine : OcrEngine {
    override suspend fun recognize(image: ByteArray) = "abcd"

    override fun isAvailable() = true
}

/** 假的平台安全存储。 */
private class FakeSecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()

    override fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun get(key: String) = values[key]

    override fun remove(key: String) {
        values.remove(key)
    }
}

/**
 * 假的教务系统认证模块。
 *
 * @param results 依次返回的登录结果，耗尽后返回成功
 */
private class FakeAuth(
    private val results: MutableList<LoginResult> = mutableListOf(),
) : AcademicAffairs.Auth {
    /** 获取验证码的次数。 */
    var captchaCount = 0
        private set

    /** 提交登录的次数。 */
    var loginCount = 0
        private set

    override suspend fun getCaptcha(): ByteArray {
        captchaCount++
        return ByteArray(0)
    }

    override suspend fun login(param: LoginParam): LoginResult {
        loginCount++
        return if (results.isEmpty()) LoginResult.Success else results.removeAt(0)
    }

    override suspend fun logout() = Unit
}

/** 假的第二课堂。 */
private class FakeExtracurricularActivity : ExtracurricularActivity {
    override val auth = object : ExtracurricularActivity.Auth {
        override suspend fun login(
            userId: String,
            password: String,
        ): String = "session-id"

        override fun restore(
            userId: String,
            sessionId: String,
        ) = Unit
    }

    override val achievement = object : ExtracurricularActivity.Achievement {
        override suspend fun getTranscript(): Transcript = error("未使用")

        override suspend fun getTranscriptLogs(): TranscriptLogs = error("未使用")
    }

    override val activity = object : ExtracurricularActivity.Activity {
        override suspend fun getEvents(): List<BasicEvent> = error("未使用")

        override suspend fun getMyEvents(state: EventState): List<MyEvent> = error("未使用")

        override suspend fun getEvent(id: Int): Event = error("未使用")
    }
}
