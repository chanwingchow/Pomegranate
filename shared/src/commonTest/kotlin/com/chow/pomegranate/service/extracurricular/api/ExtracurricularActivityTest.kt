package com.chow.pomegranate.service.extracurricular.api

import com.chow.pomegranate.LocalTestCredentials
import com.chow.pomegranate.service.extracurricular.model.EventState
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExtracurricularActivityTest {
    private lateinit var extracurricularActivity: ExtracurricularActivity
    private lateinit var username: String
    private lateinit var password: String

    @BeforeTest
    fun setUp() {
        extracurricularActivity = ExtracurricularActivity()

        val credentials = LocalTestCredentials.extracurricular ?: return
        username = credentials.userId
        password = credentials.password
    }

    /**
     * 登录测试。
     */
    @Test
    fun login() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        extracurricularActivity.auth.login(username, password)
    }

    /**
     * 测试。
     */
    @Test
    fun getTranscript() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        runWithLogin {
            val transcript = extracurricularActivity.achievement.getTranscript()

            println(transcript)
        }
    }

    /**
     * 测试。
     */
    @Test
    fun getTranscriptLogs() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        runWithLogin {
            val logs = extracurricularActivity.achievement.getTranscriptLogs()

            println(logs)
        }
    }

    /**
     * 获取活动测试。
     */
    @Test
    fun getEvents() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        runWithLogin {
            val events = extracurricularActivity.activity.getEvents()

            println(events)
        }
    }

    /**
     * 获取我的活动测试。
     */
    @Test
    fun getMyEvents() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        runWithLogin {
            val myEvents = extracurricularActivity.activity.getMyEvents(
                state = EventState.Finished,
            )

            println(myEvents)
        }
    }

    /**
     * 获取活动详情测试。
     */
    @Test
    fun getEvent() = runBlocking<Unit> {
        if (!hasExtracurricularCredentials()) return@runBlocking
        runWithLogin {
            val basicEvent = extracurricularActivity.activity.run {
                getEvents().firstOrNull()
                    ?: getMyEvents(state = EventState.Finished).first()
            }

            val event = extracurricularActivity.activity.getEvent(basicEvent.id)

            println(event)
        }
    }

    private suspend inline fun runWithLogin(block: () -> Unit) {
        extracurricularActivity.auth.login(username, password)

        block()
    }
}

/**
 * 没有配置真实账号时跳过当前用例。
 *
 * 这些用例要连校园网并用真账号登录，CI 上必然失败；跳过才能让真实回归不被淹没。
 * 必须写在 `@Test` 方法体内：JUnit 4 在 `@BeforeTest`/`@BeforeClass` 里抛出
 * AssumptionViolatedException 会记成初始化失败，而不是 skipped。
 */
private fun hasExtracurricularCredentials(): Boolean =
    LocalTestCredentials.extracurricular != null
