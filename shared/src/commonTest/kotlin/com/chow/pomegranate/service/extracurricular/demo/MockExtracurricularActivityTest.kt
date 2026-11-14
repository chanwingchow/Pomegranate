package com.chow.pomegranate.service.extracurricular.demo

import com.chow.pomegranate.service.extracurricular.model.EventState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MockExtracurricularActivityTest {
    @Test
    fun offlineTranscriptHasBothCompletedAndIncompleteCategories() = runTest {
        val api = MockExtracurricularActivity(0)
        assertEquals(MockExtracurricularActivity.SESSION_ID, api.auth.login("", ""))
        api.auth.restore("ignored-account", "ignored-session")
        val transcript = api.achievement.getTranscript()
        assertEquals(MockExtracurricularActivity.USER_ID, transcript.userId)
        assertEquals(7, transcript.items.size)
        assertEquals(7, transcript.items.map { it.categoryId }.toSet().size)
        assertTrue(transcript.items.any { it.score >= it.requiredScore })
        assertTrue(transcript.items.any { it.score < it.requiredScore })
        assertTrue(transcript.items.all { it.score == it.activityScore + it.projectScore })
        val logs = api.achievement.getTranscriptLogs()
        assertEquals(transcript.userId, logs.userId)
        assertTrue(logs.items.all { it.displayName.isNotBlank() && it.score > 0 })
        assertTrue(logs.items.all { log -> transcript.items.any { it.name == log.category } })
    }

    @Test
    fun eventsHaveMatchingDetailsAndRespectMyEventState() = runTest {
        val api = MockExtracurricularActivity(0).activity
        val events = api.getEvents()
        assertTrue(events.any { it.isOnline })
        assertTrue(events.any { !it.isOnline })
        events.forEach { basic ->
            val detail = api.getEvent(basic.id)
            assertEquals(basic.name, detail.name)
            assertEquals(basic.category, detail.category)
            assertEquals(basic.score, detail.score)
            assertTrue(detail.description.contains("虚构"))
            assertTrue(detail.peopleLimit >= detail.participantCount)
        }
        val pending = api.getMyEvents(EventState.PendingStart)
        val ongoing = api.getMyEvents(EventState.Ongoing)
        val finished = api.getMyEvents(EventState.Finished)
        assertTrue(pending.isNotEmpty() && ongoing.isNotEmpty() && finished.isNotEmpty())
        assertTrue(pending.all { it.startTime > ongoing.single().startTime })
        assertTrue(finished.all { api.getEvent(it.id).isFinished })
        assertTrue(api.getMyEvents(EventState.Enrolled).map { it.id }.containsAll(pending.map { it.id }))
        assertFailsWith<IllegalStateException> { api.getEvent(-1) }
    }

    @Test
    fun responseDelayIsCancellableAndDoesNotRequireLogin() = runTest {
        val api = MockExtracurricularActivity()
        val request = async { api.achievement.getTranscript() }
        runCurrent()
        advanceTimeBy(349)
        assertFalse(request.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(request.await().items.isNotEmpty())
        val cancelled = async { api.activity.getEvents() }
        runCurrent()
        cancelled.cancel()
        cancelled.join()
        assertTrue(cancelled.isCancelled)
        assertTrue(api.activity.getEvents().isNotEmpty())
    }
}
