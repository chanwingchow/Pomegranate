package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.session.SessionManager

internal class MemorySchoolTimetableStorage : SchoolTimetableStorage {
    private val entries = mutableMapOf<String, Map<String, SavedSchoolTimetable>>()
    override suspend fun read(owner: String) = entries[owner].orEmpty()
    override suspend fun write(owner: String, values: Map<String, SavedSchoolTimetable>) { entries[owner] = values.toMap() }
}

internal fun testSchoolTimetables(session: SessionManager, academic: AcademicAffairs = MockAcademicAffairs(0, 0)) =
    SchoolTimetableRepository(academic, MemorySchoolTimetableStorage(), session)
