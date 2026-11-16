package com.chow.pomegranate.service.extracurricular.demo

import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.*
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** 离线第二课堂服务。成绩、活动和记录均为虚构数据，不读取真实凭据。 */
class MockExtracurricularActivity(
    private val responseDelayMillis: Long = 350L,
) : ExtracurricularActivity {
    init { require(responseDelayMillis >= 0) }

    companion object {
        const val USER_ID = "演示账号"
        const val SESSION_ID = "demo-extracurricular-session"
    }

    private val now = Clock.System.now().toEpochMilliseconds()
    private val day = 86_400_000L
    private val categories = listOf("思想政治与道德素养", "创新创业与实践能力", "社会责任与志愿服务",
        "团队协作与交流能力", "文化艺术与审美素养", "身心健康与体育素养", "职业发展与综合能力")
    // 每项为一次独立历史活动的实际得分，成绩由这些记录汇总，不把总分平均拆分。
    private val historyScores = listOf(
        listOf(2, 2, 2, 2, 1, 1), listOf(1, 1, 1, 1), listOf(3, 3, 3, 3, 2, 2),
        listOf(1, 1, 1, 1, 1, 1), listOf(2, 1, 1, 1, 1, 1, 1), listOf(1, 1, 1, 1), listOf(1, 1, 1, 1, 1),
    )
    private val history = historyScores.flatMapIndexed { category, scores ->
        val title = listOf("青年理论研学", "创新实践训练", "社区志愿服务", "团队交流工作坊", "校园艺术体验", "运动健康课堂", "职业能力训练")[category]
        scores.mapIndexed { index, score ->
            val start = now - (30 + category * 7 + index * 2) * day
            BasicEvent(2000 + category * 100 + index, "$title · 第 ${index + 1} 场（演示）", categories[category],
                score.toDouble(), start, start + 7_200_000, "学生发展中心（演示）", null,
                "实践活动", false, 32.0)
        }
    }
    private val transcript = Transcript(USER_ID, categories.mapIndexed { index, name ->
        val activityScore = history.filter { it.category == name }.sumOf { it.score }
        ExtracurricularGrade(index + 1, name, activityScore + 2.0, 10.0, 30.0, activityScore, 2.0,
            awardScore = 0.0, averageScore = 8.0, isRequired = 1, termMinScore = 2.0)
    })
    private val events = listOf(
        BasicEvent(1001, "校园志愿服务体验（演示）", categories[2], 2.0, now + day, now + day + 7_200_000,
            "青年志愿者协会（演示）", null, "志愿服务", false, 36.0),
        BasicEvent(1002, "创新实践工作坊（演示）", categories[1], 3.0, now + 3 * day, now + 3 * day + 10_800_000,
            "创新实践中心（演示）", null, "实践活动", false, 24.0),
        BasicEvent(1003, "职业生涯规划讲座（演示）", categories[6], 1.0, now - 3_600_000, now + 3_600_000,
            "学生发展中心（演示）", null, "专题讲座", true, 72.0),
        BasicEvent(1004, "校园文化分享会（演示）", categories[4], 2.0, now - 7 * day, now - 7 * day + 7_200_000,
            "校园文化社（演示）", null, "文化活动", false, 48.0),
    ) + (0 until 60).map { index ->
        val phase = index % 3
        val start = when (phase) { 0 -> now + (index / 3 + 2) * day; 1 -> now - (index + 1) * 60_000L; else -> now - (index / 3 + 2) * day }
        val end = if (phase == 1) now + (index + 1) * 60_000L else start + 7_200_000L
        val title = listOf("青年读书分享", "社区志愿行动", "创新创意工坊", "校园艺术展", "运动健康课堂", "求职能力训练", "社会实践交流")[index % 7]
        BasicEvent(1100 + index, "$title · 第 ${index / 7 + 1} 期（演示）", categories[index % categories.size],
            1.0 + index % 3, start, end, "学生发展中心（演示）", null,
            if (index % 2 == 0) "实践活动" else "专题讲座", index % 4 == 0, 20.0 + index % 60)
    } + history

    private fun dateTime(epochMillis: Long) = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).let {
            "${it.date} ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
        }

    override val auth = object : ExtracurricularActivity.Auth {
        override suspend fun login(userId: String, password: String): String {
            delay(responseDelayMillis)
            return SESSION_ID
        }
        // 演示服务无需真实会话，且不会保存传入的账号或会话。
        override fun restore(userId: String, sessionId: String) = Unit
    }

    override val achievement = object : ExtracurricularActivity.Achievement {
        override suspend fun getTranscript(): Transcript {
            delay(responseDelayMillis)
            return transcript
        }
        override suspend fun getTranscriptLogs(): TranscriptLogs {
            delay(responseDelayMillis)
            val activities = history.map { event ->
                TranscriptLog(event.name, null, null, event.category, event.score, event.endTime + day,
                    "演示学期", sourceType = 1, note = "虚构的活动成绩记录，仅供演示。")
            }
            val projects = transcript.items.mapIndexed { index, grade ->
                TranscriptLog(null, "${grade.name}实践项目（演示）", null, grade.name, grade.projectScore,
                    now - (index + 1) * day, "演示学期", sourceType = 2, note = "虚构的项目成绩记录，仅供演示。")
            }
            return TranscriptLogs(USER_ID, (activities + projects).sortedByDescending { it.time })
        }
    }

    override val activity = object : ExtracurricularActivity.Activity {
        override suspend fun getEvents(): List<BasicEvent> {
            delay(responseDelayMillis)
            return events
        }
        override suspend fun getMyEvents(state: EventState): List<MyEvent> {
            delay(responseDelayMillis)
            val selected = when (state) {
                EventState.Enrolled -> events
                EventState.PendingStart -> events.filter { it.startTime > now }
                EventState.Ongoing -> events.filter { it.startTime <= now && it.endTime > now }
                EventState.Finished -> events.filter { it.endTime <= now }
            }
            return selected.map { event ->
                MyEvent(event.id, event.name, event.category, event.score, event.startTime, event.endTime,
                    event.organizer, event.imageUrl, event.eventType, event.isOnline,
                    event.startTime - 3_600_000, auditStatus = 1, auditNote = "演示报名记录")
            }
        }
        override suspend fun getEvent(id: Int): Event {
            delay(responseDelayMillis)
            val event = events.firstOrNull { it.id == id } ?: error("演示活动不存在。")
            return Event(id = event.id, name = event.name,
                description = "本活动及参与记录均为虚构数据，用于预览第二课堂功能。无需报名，不产生真实学分。",
                category = event.category, score = event.score,
                location = if (event.isOnline) "线上会议室（演示）" else "学生活动中心（演示）",
                coverUrl = "", organizer = event.organizer, admin = "演示管理员", phoneNumber = "",
                teacher = "演示指导老师", eventType = event.eventType,
                startTime = dateTime(event.startTime), endTime = dateTime(event.endTime),
                enrollEndTime = dateTime(event.startTime - 3_600_000), trainingHours = event.score,
                needSubmit = id == 1002, peopleLimit = 100, participantCount = event.participantCount.toInt(),
                note = "仅供演示", isFinished = event.endTime < now, applyStatus = 1,
                creditStatus = if (event.endTime < now) 1 else 0, statusAll = if (event.endTime < now) 3 else 1,
                attachments = emptyList())
        }
    }
}
