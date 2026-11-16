package com.chow.pomegranate.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppPreferenceDaoTest {
    @Test
    fun putObserveReplaceAndRemovePreference() = runBlocking {
        val databaseFile = Files.createTempFile("pomegranate-test-", ".db")
        val database = Room.databaseBuilder<AppDatabase>(databaseFile.toString())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val dao = database.preferenceDao()
            assertNull(dao.value("theme"))

            dao.put(AppPreferenceEntity("theme", "dark"))
            assertEquals("dark", dao.value("theme"))
            assertEquals("dark", dao.observeValue("theme").first())

            dao.put(AppPreferenceEntity("theme", "light"))
            assertEquals("light", dao.value("theme"))

            dao.remove("theme")
            assertNull(dao.value("theme"))
        } finally {
            database.close()
            databaseFile.deleteIfExists()
        }
    }
}
