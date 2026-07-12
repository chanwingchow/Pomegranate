package com.chow.pomegranate.service.extracurricular.api

import com.chow.pomegranate.service.extracurricular.model.EventState
import io.ktor.utils.io.readText
import kotlinx.coroutines.runBlocking
import kotlinx.io.SystemLineSeparator
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExtracurricularActivityTest {
    private lateinit var extracurricularActivity: ExtracurricularActivity
    private lateinit var username: String
    private lateinit var password: String

    @BeforeTest
    fun setUp() {
        extracurricularActivity = ExtracurricularActivity()

        // 读取 local.properties 里面的用户配置
        // user.name=学号
        // user.password=密码
        val properties = SystemFileSystem.source(Path("../local.properties"))
            .buffered()
            .readText()
            .split(SystemLineSeparator)
            .filter { it.startsWith("user") }
            .associate {
                val (key, value) = it.split("=")
                key.trim() to value.trim()
            }

        username = properties["user.name"]!!
        password = properties["user.extracurricularPassword"]!!
    }

    /**
     * 登录测试。
     */
    @Test
    fun login() = runBlocking {
        extracurricularActivity.auth.login(username, password)
    }

    /**
     * 测试。
     */
    @Test
    fun getTranscript() = runBlocking {
        runWithLogin {
            val transcript = extracurricularActivity.achievement.getTranscript()

            println(transcript)
        }
    }

    /**
     * 测试。
     */
    @Test
    fun getTranscriptLogs() = runBlocking {
        runWithLogin {
            val logs = extracurricularActivity.achievement.getTranscriptLogs()

            println(logs)
        }
    }

    /**
     * 获取活动测试。
     */
    @Test
    fun getEvents() = runBlocking {
        runWithLogin {
            val events = extracurricularActivity.activity.getEvents()

            println(events)
        }
    }

    /**
     * 获取我的活动测试。
     */
    @Test
    fun getMyEvents() = runBlocking {
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
    fun getEvent() = runBlocking {
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