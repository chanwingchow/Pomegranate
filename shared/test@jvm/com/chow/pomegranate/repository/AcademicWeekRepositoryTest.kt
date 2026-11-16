package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.PreferenceKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class AcademicWeekRepositoryTest {
    @Test fun persistsTheMondayAnchorAndResetsItOnlyWhenTheUserRecalibrates() = runBlocking {
        withWeekDatabase { preferences ->
            val repository = AcademicWeekRepository(preferences)
            repository.set(8, LocalDate(2026, 11, 15))
            val reopened = AcademicWeekRepository(preferences)
            assertEquals(8, reopened.get(LocalDate(2026, 11, 15)))
            assertEquals(9, reopened.get(LocalDate(2026, 11, 16)))
            assertEquals(11, reopened.get(LocalDate(2026, 12, 6)))
            assertEquals("8", preferences.get(PreferenceKeys.WEEK))
            assertEquals(9, reopened.observe(MutableStateFlow(LocalDate(2026, 11, 16))).first())
            reopened.set(3, LocalDate(2026, 11, 18))
            assertEquals(3, reopened.get(LocalDate(2026, 11, 22)))
            assertEquals(4, reopened.get(LocalDate(2026, 11, 23)))
        }
    }

    @Test fun legacyNumericWeekMigratesOnceWithoutChangingItsCurrentMeaning() = runBlocking {
        withWeekDatabase { preferences ->
            preferences.set(PreferenceKeys.WEEK, "6")
            val repository = AcademicWeekRepository(preferences)
            assertEquals(6, repository.get(LocalDate(2026, 11, 15)))
            assertEquals(7, AcademicWeekRepository(preferences).get(LocalDate(2026, 11, 16)))
        }
    }

    @Test fun anOpenScreenReceivesTheNewWeekAndManualRecalibration() = runBlocking {
        withWeekDatabase { preferences ->
            val repository = AcademicWeekRepository(preferences)
            repository.set(8, LocalDate(2026, 11, 15))
            val dates = MutableStateFlow(LocalDate(2026, 11, 15))
            val updates = Channel<Int>(Channel.UNLIMITED)
            val job = launch { repository.observe(dates).collect { updates.send(it) } }
            try {
                withTimeout(5_000) {
                    assertEquals(8, updates.receive())
                    dates.value = LocalDate(2026, 11, 16)
                    assertEquals(9, updates.receive())
                    repository.set(3, dates.value)
                    assertEquals(3, updates.receive())
                }
            } finally { job.cancelAndJoin(); updates.close() }
        }
    }
}

private suspend fun withWeekDatabase(block: suspend (AppPreferenceRepository) -> Unit) {
    val file = Files.createTempFile("academic-week-", ".db")
    val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO).build()
    try { block(AppPreferenceRepository(db)) } finally { db.close(); file.deleteIfExists() }
}
