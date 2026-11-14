package com.chow.pomegranate.service.extracurricular.internal

import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.EventState

/** 每次请求选择服务，仓库缓存模块引用后切换模式也仍然生效。 */
class SwitchingExtracurricularActivity(
    private val real: ExtracurricularActivity,
    private val demo: ExtracurricularActivity,
    private val mode: DemoModeRepository,
) : ExtracurricularActivity {
    private suspend fun backend() = if (mode.isEnabled()) demo else real

    override val auth = object : ExtracurricularActivity.Auth {
        override suspend fun login(userId: String, password: String) = backend().auth.login(userId, password)
        override fun restore(userId: String, sessionId: String) {
            (if (mode.enabled.value == true) demo else real).auth.restore(userId, sessionId)
        }
    }
    override val achievement = object : ExtracurricularActivity.Achievement {
        override suspend fun getTranscript() = backend().achievement.getTranscript()
        override suspend fun getTranscriptLogs() = backend().achievement.getTranscriptLogs()
    }
    override val activity = object : ExtracurricularActivity.Activity {
        override suspend fun getEvents() = backend().activity.getEvents()
        override suspend fun getMyEvents(state: EventState) = backend().activity.getMyEvents(state)
        override suspend fun getEvent(id: Int) = backend().activity.getEvent(id)
    }
}
