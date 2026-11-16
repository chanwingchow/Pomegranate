package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TimetableRepositoryTest {
    @Test fun reusesEachAccountAndSemesterUntilAnExplicitRefresh() = runTest {
        val demo = MockAcademicAffairs(0, 0)
        var calls = 0
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getTimetable(semester: Semester) = demo.enrollment.getTimetable(semester).also { calls++ }
            }
        }
        val session = TimetableTestSession()
        val repository = TimetableRepository(service, session)
        val term = Semester.now()
        val first = repository.getTimetable(term)
        assertSame(first, repository.getTimetable(term))
        assertEquals(1, calls)
        repository.getTimetable(term, refresh = true)
        assertEquals(2, calls)
        repository.getTimetable(term.previous())
        session.userId.value = "another-account"
        repository.getTimetable(term)
        assertEquals(4, calls)
        session.userId.value = "demo"
        val real = repository.getTimetable(term)
        assertEquals(4, calls)
        session.accounts.value = listOf(Account("demo", true, isDemo = true))
        repository.getTimetable(term)
        assertEquals(5, calls)
        session.accounts.value = emptyList()
        assertSame(real, repository.getTimetable(term))
        TimetableRepository(service, session).getTimetable(term)
        assertEquals(6, calls) // A new application session fetches once again.
    }

    @Test fun concurrentInitialLoadsShareTheRequestAndFailuresDoNotReplaceTheCache() = runTest {
        val demo = MockAcademicAffairs(30, 30)
        var calls = 0
        var offline = false
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getTimetable(semester: Semester) = run {
                    calls++
                    check(!offline) { "offline" }
                    demo.enrollment.getTimetable(semester)
                }
            }
        }
        val repository = TimetableRepository(service, TimetableTestSession())
        val a = async { repository.getTimetable() }
        val b = async { repository.getTimetable() }
        val first = a.await()
        assertSame(first, b.await())
        assertEquals(1, calls)
        offline = true
        assertFailsWith<IllegalStateException> { repository.getTimetable(refresh = true) }
        assertSame(first, repository.getTimetable())
        assertSame(first, repository.current.value)
        assertEquals(2, calls)
    }

    @Test fun switchingAccountsDuringALoadDoesNotPublishThePreviousAccount() = runTest {
        val demo = MockAcademicAffairs(0, 0)
        val session = TimetableTestSession()
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getTimetable(semester: Semester) = demo.enrollment.getTimetable(semester).also {
                    session.userId.value = "another-account"
                }
            }
        }
        val repository = TimetableRepository(service, session)
        assertFailsWith<IllegalStateException> { repository.getTimetable() }
        assertNull(repository.current.value)
    }
}

internal class TimetableTestSession : SessionManager {
    override val userId = MutableStateFlow<String?>("demo")
    override val state = MutableStateFlow<SessionState>(SessionState.Active)
    override val accounts = MutableStateFlow(emptyList<Account>())
    override suspend fun awaitLogin(): Boolean = false
    override suspend fun getCaptcha(): ByteArray = error("unused")
    override suspend fun signIn(request: LoginRequest) = Unit
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) = Unit
    override suspend fun autoLogin() = Unit
    override suspend fun switchAccount(userId: String) = Unit
    override suspend fun deleteAccount(userId: String) = Unit
    override suspend fun refresh() = Unit
    override suspend fun logout() = Unit
}
