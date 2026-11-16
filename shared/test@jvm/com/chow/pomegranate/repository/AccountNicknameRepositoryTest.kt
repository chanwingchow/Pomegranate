package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class AccountNicknameRepositoryTest {
    @Test fun namesPersistPerAccountAndDemoStatusAndCanBeResetWithoutChangingOtherAccounts() = runBlocking {
        val file = Files.createTempFile("account-nicknames-", ".db")
        val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO).build()
        try {
            val preferences = AppPreferenceRepository(db)
            val repository = AccountNicknameRepository(preferences)
            repository.save("A", false, "  昵称甲  ")
            repository.save("B", false, "昵称乙")
            repository.save("A", true, "演示昵称")
            val reopened = AccountNicknameRepository(preferences)
            val names = reopened.names.first()
            assertEquals("昵称甲", names[accountNicknameKey("A", false)])
            assertEquals("昵称乙", names[accountNicknameKey("B", false)])
            assertEquals("演示昵称", names[accountNicknameKey("A", true)])
            assertFailsWith<IllegalArgumentException> { repository.save("A", false, "太长".repeat(20)) }
            assertEquals(names, reopened.names.first())
            repository.save("A", false, " ")
            val reset = reopened.names.first()
            assertNull(reset[accountNicknameKey("A", false)])
            assertEquals("昵称乙", reset[accountNicknameKey("B", false)])
            assertEquals("演示昵称", reset[accountNicknameKey("A", true)])
        } finally { db.close(); file.deleteIfExists() }
    }
}
