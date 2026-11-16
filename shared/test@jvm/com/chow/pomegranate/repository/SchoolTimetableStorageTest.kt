package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class SchoolTimetableStorageTest {
    @Test fun persistsAllRawSessionsAndSeparatesAccountsWhenOneSemesterIsRemoved() = runBlocking {
        val file = Files.createTempFile("school-timetable-storage-", ".db")
        val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO).build()
        try {
            val preferences = AppPreferenceRepository(db)
            val storage = PreferenceSchoolTimetableStorage(preferences)
            val term = Semester.now()
            val data = MockAcademicAffairs(0, 0).enrollment.getCourseTimetable(term)
            val current = SavedSchoolTimetable(data, 100L)
            val previous = SavedSchoolTimetable(data.copy(semester = term.previous()), 50L)
            storage.write("A", mapOf(term.toString() to current, term.previous().toString() to previous))
            storage.write("B", mapOf(term.toString() to current))
            val reopened = PreferenceSchoolTimetableStorage(preferences)
            assertEquals(data, reopened.read("A").getValue(term.toString()).timetable)
            assertEquals(160, reopened.read("A").getValue(term.toString()).timetable.courses.size)
            reopened.write("A", reopened.read("A") - term.previous().toString())
            assertEquals(mapOf(term.toString() to current), storage.read("A"))
            assertEquals(mapOf(term.toString() to current), storage.read("B"))
        } finally { db.close(); file.deleteIfExists() }
    }
}
