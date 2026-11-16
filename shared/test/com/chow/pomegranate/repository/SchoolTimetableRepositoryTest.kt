package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SchoolTimetableRepositoryTest {
    @Test fun refreshesOncePerAccountAndSemesterAndDeletesOnlyConfirmedLocalData() = runTest {
        val demo = MockAcademicAffairs(0, 0)
        var calls = 0
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getCourseTimetable(semester: Semester) = demo.enrollment.getCourseTimetable(semester).also { calls++ }
            }
        }
        val session = CacheTestSession()
        val storage = MemorySchoolTimetableStorage()
        val repository = SchoolTimetableRepository(service, storage, session)
        val term = Semester.now()
        val first = repository.get(term)
        assertSame(first, repository.get(term))
        assertEquals(1, calls)
        repository.get(term, refresh = true)
        assertEquals(2, calls)
        repository.get(term.previous())
        val ownerA = repository.owner()
        assertEquals(2, repository.readSaved().size)
        session.userId.value = "B"
        repository.get(term)
        assertEquals(4, calls)
        assertEquals(1, repository.readSaved().size)
        assertFailsWith<IllegalStateException> { repository.delete(ownerA, term) }
        session.userId.value = "A"
        repository.delete(ownerA, term.previous())
        assertEquals(listOf(term), repository.readSaved().map { it.timetable.semester })
        assertSame(first.semester, repository.get(term).semester)
        assertEquals(4, calls)
        val restarted = SchoolTimetableRepository(service, storage, session)
        restarted.get(term)
        restarted.get(term)
        assertEquals(5, calls)
        assertEquals(listOf(term), restarted.readSaved().map { it.timetable.semester })
    }

    @Test fun anOfflineRefreshRetainsThePreviouslySavedSnapshot() = runTest {
        val demo = MockAcademicAffairs(0, 0)
        var offline = false
        val service = object : AcademicAffairs by demo {
            override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                override suspend fun getCourseTimetable(semester: Semester): com.chow.pomegranate.service.academic.affairs.model.CourseTimetable {
                    if (offline) error("offline")
                    return demo.enrollment.getCourseTimetable(semester)
                }
            }
        }
        val session = CacheTestSession()
        val storage = MemorySchoolTimetableStorage()
        val term = Semester.now()
        val expected = SchoolTimetableRepository(service, storage, session).get(term)
        offline = true
        assertEquals(expected, SchoolTimetableRepository(service, storage, session).get(term))
        assertEquals(1, storage.read(SchoolTimetableRepository(service, storage, session).owner()).size)
    }
}

private class CacheTestSession : SessionManager {
    override val userId = MutableStateFlow<String?>("A")
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
