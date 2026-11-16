package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class TimetableCustomizationRepositoryTest {
    @Test fun serverRefreshDoesNotOverwriteLocalRoomOrMultilineNotes() = runBlocking {
        withDatabase { db ->
            val demo = MockAcademicAffairs(0, 0)
            var source = demo.enrollment.getTimetable(Semester.now())
            val service = object : AcademicAffairs by demo {
                override val enrollment = object : AcademicAffairs.Enrollment by demo.enrollment {
                    override suspend fun getTimetable(semester: Semester) = source
                }
            }
            val timetable = TimetableRepository(service, TimetableTestSession())
            val preferences = AppPreferenceRepository(db)
            val customizations = TimetableCustomizationRepository(preferences)
            val imported = timetable.getTimetable()
            val lesson = imported.courses.first().lesson(imported.ownerKey())
            customizations.save(lesson, "本地教室 A999", "第一行\n第二行", name = "数据结构·自定义")
            source = source.copy(courses = source.courses.mapIndexed { i, course ->
                if (i == 0) course.copy(classroom = "服务器新教室 B888", teacher = "新教师", weeksString = "1-20周") else course
            })
            val refreshed = timetable.getTimetable(refresh = true)
            val updated = refreshed.courses.first().lesson(refreshed.ownerKey())
            assertEquals(lesson.key, updated.key)
            assertEquals("服务器新教室 B888", updated.classroom)
            assertEquals("新教师", updated.teacher)
            val saved = TimetableCustomizationRepository(preferences).get(refreshed.ownerKey()).getValue(updated.key)
            assertEquals("本地教室 A999", saved.classroom)
            assertEquals("数据结构·自定义", saved.name)
            assertEquals(lesson.name, updated.name)
            assertEquals("第一行\n第二行", saved.note)
            assertEquals(refreshed, timetable.current.value)
        }
    }

    @Test fun noteOnlyDoesNotPinTheServerRoomAndRestoreRemovesTheOverride() = runBlocking {
        withDatabase { db ->
            val timetable = MockAcademicAffairs(0, 0).enrollment.getTimetable(Semester.now())
            val lesson = timetable.courses.first().lesson(timetable.ownerKey())
            val repository = TimetableCustomizationRepository(AppPreferenceRepository(db))
            repository.save(lesson, lesson.classroom.orEmpty(), "注意带书")
            assertNull(repository.get(lesson.owner).getValue(lesson.key).classroom)
            assertEquals("注意带书", repository.get(lesson.owner).getValue(lesson.key).note)
            repository.save(lesson, lesson.classroom.orEmpty(), "")
            assertTrue(repository.get(lesson.owner).isEmpty())
        }
    }

    @Test fun courseNamesAcceptLongTitlesAndKeepTheOriginalLessonIdentity() = runBlocking {
        withDatabase { db ->
            val timetable = MockAcademicAffairs(0, 0).enrollment.getTimetable(Semester.now())
            val lesson = timetable.courses.first().lesson(timetable.ownerKey())
            val preferences = AppPreferenceRepository(db)
            val repository = TimetableCustomizationRepository(preferences)
            val title = "毛泽东思想和中国特色社会主义理论体系概论".repeat(5)
            repository.save(lesson, lesson.classroom.orEmpty(), "", title)
            assertEquals(title, TimetableCustomizationRepository(preferences).get(lesson.owner).getValue(lesson.key).name)
            assertFailsWith<IllegalArgumentException> { repository.save(lesson, "", "", "   ") }
            assertEquals(title, repository.get(lesson.owner).getValue(lesson.key).name)
            repository.save(lesson, lesson.classroom.orEmpty(), "", lesson.name)
            assertTrue(repository.get(lesson.owner).isEmpty())
            // 添加名称字段之前保存的教室和备注仍可读取。
            val legacy = kotlinx.serialization.json.Json.encodeToString(mapOf(lesson.key to mapOf("classroom" to "旧教室", "note" to "旧备注")))
            preferences.set("timetable_customizations:${lesson.owner}", legacy)
            val saved = repository.get(lesson.owner).getValue(lesson.key)
            assertNull(saved.name)
            assertEquals("旧教室", saved.classroom)
            assertEquals("旧备注", saved.note)
        }
    }

    @Test fun editsAreIsolatedByAccountSemesterAndLessonTime() = runBlocking {
        withDatabase { db ->
            val timetable = MockAcademicAffairs(0, 0).enrollment.getTimetable(Semester.now())
            val lesson = timetable.courses.first().lesson(timetable.ownerKey())
            val repository = TimetableCustomizationRepository(AppPreferenceRepository(db))
            repository.save(lesson, "本地教室", "备注")
            assertTrue(repository.get(timetable.copy(userId = "another-user").ownerKey()).isEmpty())
            assertTrue(repository.get(timetable.copy(semester = timetable.semester.next()).ownerKey()).isEmpty())
            assertNotEquals(lesson.key, timetable.courses.first().copy(sections = setOf(9, 10)).lesson(lesson.owner).key)
        }
    }
}

private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
    val file = Files.createTempFile("timetable-customization-", ".db")
    val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO).build()
    try { block(db) } finally { db.close(); file.deleteIfExists() }
}
