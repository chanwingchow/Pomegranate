package com.chow.pomegranate.demo

import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.UserEntity
import com.chow.pomegranate.di.AppModule
import com.chow.pomegranate.di.AUTH_ACADEMIC_AFFAIRS
import com.chow.pomegranate.repository.AcademicProgramRepository
import com.chow.pomegranate.repository.CourseSelectionRepository
import com.chow.pomegranate.ui.screen.settings.SettingsViewModel
import com.chow.pomegranate.tool.ocr.OcrEngine
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.core.qualifier.named
import org.koin.plugin.module.dsl.modules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.internal.SwitchingAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.internal.SwitchingExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.EventState
import com.chow.pomegranate.session.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DemoModeIntegrationTest {
    @Test
    fun koinResolvesSettingsRepositoriesAndTheSameMockInstance() = runTest {
        withDatabase { db ->
            val app = koinApplication {
                modules(AppModule::class)
                modules(module {
                    single<AppDatabase> { db }
                    single<SecureStore> { object : SecureStore {
                        override fun get(key: String): String? = null
                        override fun put(key: String, value: String) = error("演示模式不应保存真实凭据")
                        override fun remove(key: String) = error("演示模式不应删除真实凭据")
                    } }
                    single<OcrEngine> { object : OcrEngine {
                        override suspend fun recognize(image: ByteArray) = error("演示模式无需识别验证码")
                        override fun isAvailable() = false
                    } }
                    single<MockAcademicAffairs> { MockAcademicAffairs(0, 0) }
                })
            }
            var session: SessionManager? = null
            try {
                val koin = app.koin
                koin.get<DemoModeRepository>().setEnabled(true)
                session = koin.get<SessionManager>()
                assertTrue(session is DemoSessionManager)
                assertTrue(koin.get<AcademicAffairs>() is SwitchingAcademicAffairs)
                val extracurricular = koin.get<ExtracurricularActivity>()
                assertTrue(extracurricular is SwitchingExtracurricularActivity)
                assertEquals(MockExtracurricularActivity.USER_ID, extracurricular.achievement.getTranscript().userId)
                // 真实会话使用独立认证客户端，不允许被演示认证污染。
                assertFalse(koin.get<AcademicAffairs>(named(AUTH_ACADEMIC_AFFAIRS)) is SwitchingAcademicAffairs)
                koin.get<SettingsViewModel>().viewModelScope.cancel()
                session.autoLogin()
                assertEquals(false, session.awaitLogin())
                assertEquals(MockAcademicAffairs.USER_ID, koin.get<AcademicProgramRepository>().getAcademicProgram().userId)
                val courses = koin.get<CourseSelectionRepository>()
                val course = courses.search("人工智能").single()
                assertTrue(courses.select(course.id).isSuccess)
                assertTrue(koin.get<MockAcademicAffairs>().courseSystem.getSelectedCourses().any { it.id == course.id })
            } finally {
                // 应用单例持有进程级观察者；测试结束前停止，避免在数据库关闭后继续查询。
                listOfNotNull(session, app.koin.getOrNull<SessionManagerImpl>()).forEach { manager ->
                    val field = manager.javaClass.getDeclaredField("scope").apply { isAccessible = true }
                    (field.get(manager) as CoroutineScope).coroutineContext[Job]?.cancelAndJoin()
                }
                app.close()
            }
        }
    }

    @Test
    fun extracurricularSwitchesCapturedModulesWithoutTouchingRealServiceInDemo() = runTest {
        withDatabase { db ->
            val mode = DemoModeRepository(AppPreferenceRepository(db))
            val demo = MockExtracurricularActivity(0)
            var realCalls = 0
            val real = object : ExtracurricularActivity {
                override val auth = object : ExtracurricularActivity.Auth {
                    override suspend fun login(userId: String, password: String): String { realCalls++; return "real-session" }
                    override fun restore(userId: String, sessionId: String) { realCalls++ }
                }
                override val achievement = object : ExtracurricularActivity.Achievement {
                    override suspend fun getTranscript() = demo.achievement.getTranscript().copy(userId = "real-user").also { realCalls++ }
                    override suspend fun getTranscriptLogs() = demo.achievement.getTranscriptLogs().also { realCalls++ }
                }
                override val activity = object : ExtracurricularActivity.Activity {
                    override suspend fun getEvents() = demo.activity.getEvents().also { realCalls++ }
                    override suspend fun getMyEvents(state: EventState) = demo.activity.getMyEvents(state).also { realCalls++ }
                    override suspend fun getEvent(id: Int) = demo.activity.getEvent(id).also { realCalls++ }
                }
            }
            val router = SwitchingExtracurricularActivity(real, demo, mode)
            val achievement = router.achievement
            val activity = router.activity
            val auth = router.auth
            mode.setEnabled(true)
            assertEquals(MockExtracurricularActivity.SESSION_ID, auth.login("", ""))
            auth.restore("ignored", "ignored")
            assertEquals(MockExtracurricularActivity.USER_ID, achievement.getTranscript().userId)
            assertTrue(achievement.getTranscriptLogs().items.isNotEmpty())
            assertTrue(activity.getMyEvents(EventState.Finished).isNotEmpty())
            assertEquals(activity.getEvents().first().id, activity.getEvent(activity.getEvents().first().id).id)
            assertEquals(0, realCalls)
            mode.setEnabled(false)
            assertEquals("real-user", achievement.getTranscript().userId)
            achievement.getTranscriptLogs()
            activity.getEvents()
            activity.getMyEvents(EventState.Finished)
            activity.getEvent(1001)
            assertEquals("real-session", auth.login("account", "password"))
            auth.restore("account", "session")
            assertEquals(7, realCalls)
        }
    }

    @Test
    fun switchRoutesPreviouslyCapturedModulesAndRemembersMode() = runTest {
        withDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val mode = DemoModeRepository(preferences)
            val demo = MockAcademicAffairs(0, 0)
            val real = object : AcademicAffairs by demo {
                override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                    override suspend fun getTimetable(semester: Semester) =
                        demo.enrollment.getTimetable(semester).copy(userId = "real-user")
                }
                override val academic = object : AcademicAffairs.Academic by demo.academic {
                    override suspend fun getAcademicProgram() = demo.academic.getAcademicProgram().copy(userId = "real-user")
                }
            }
            val router = SwitchingAcademicAffairs(real, demo, mode)
            val enrollment = router.enrollment
            val academic = router.academic
            assertFalse(mode.isEnabled())
            assertEquals("real-user", enrollment.getTimetable(Semester.now()).userId)
            mode.setEnabled(true)
            assertTrue(DemoModeRepository(preferences).isEnabled())
            assertEquals(MockAcademicAffairs.USER_ID, enrollment.getTimetable(Semester.now()).userId)
            assertEquals(MockAcademicAffairs.USER_ID, academic.getAcademicProgram().userId)
            mode.setEnabled(false)
            assertFalse(DemoModeRepository(preferences).isEnabled())
            assertEquals("real-user", academic.getAcademicProgram().userId)
        }
    }

    @Test
    fun persistedDemoNeverUsesRealSessionAndExitingRestoresAccount() = runTest {
        withDatabase { db ->
            val user = UserEntity("real-user", "", "", true, 0L)
            db.userDao().save(user)
            val preferences = AppPreferenceRepository(db)
            DemoModeRepository(preferences).setEnabled(true)
            // 模拟应用重启：重新读取已保存的开关，不先发起真实自动登录。
            val mode = DemoModeRepository(preferences)
            val real = RecordingSessionManager()
            val session = DemoSessionManager(real, MockAcademicAffairs(0, 0), mode, backgroundScope)
            session.autoLogin()
            assertEquals(false, session.awaitLogin())
            session.refresh()
            session.login("ignored", "ignored", "ignored", "")
            runCurrent()
            assertEquals(0, real.calls)
            assertEquals(SessionState.Active, session.state.value)
            assertEquals(MockAcademicAffairs.USER_ID, session.userId.value)
            assertEquals(listOf(Account(MockAcademicAffairs.USER_ID, true)), session.accounts.value)
            assertFailsWith<IllegalStateException> { session.deleteAccount("real-user") }
            assertFailsWith<IllegalStateException> { session.switchAccount("real-user") }
            assertEquals(user, db.userDao().user("real-user"))
            // 演示中的退出动作只关闭开关，不登出真实账号。
            session.logout()
            runCurrent()
            assertFalse(mode.isEnabled())
            assertEquals(0, real.calls)
            assertEquals("real-user", session.userId.value)
            assertEquals(real.accounts.value, session.accounts.value)
            session.autoLogin()
            assertEquals(1, real.calls)
        }
    }
}

private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
    val file = Files.createTempFile("pomegranate-demo-test-", ".db")
    val db = Room.databaseBuilder<AppDatabase>(file.toString())
        .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    try { block(db) } finally { db.close(); file.deleteIfExists() }
}

private class RecordingSessionManager : SessionManager {
    var calls = 0
    override val userId = MutableStateFlow<String?>("real-user")
    override val state = MutableStateFlow<SessionState>(SessionState.Active)
    override val accounts = MutableStateFlow(listOf(Account("real-user", true)))
    override suspend fun getCaptcha(): ByteArray { calls++; return byteArrayOf() }
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) { calls++ }
    override suspend fun autoLogin() { calls++ }
    override suspend fun switchAccount(userId: String) { calls++ }
    override suspend fun deleteAccount(userId: String) { calls++ }
    override suspend fun refresh() { calls++ }
    override suspend fun logout() { calls++ }
    override suspend fun awaitLogin(): Boolean { calls++; return false }
}
