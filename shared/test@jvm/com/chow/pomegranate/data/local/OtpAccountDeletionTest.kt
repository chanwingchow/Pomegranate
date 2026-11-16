package com.chow.pomegranate.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.repository.OtpAccountRepository
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class OtpAccountDeletionTest {
    @Test
    fun deleteOnlyConfirmedAccountsAndPreserveOtherData() = runBlocking {
        val file = Files.createTempFile("pomegranate-otp-delete-", ".db")
        val database = Room.databaseBuilder<AppDatabase>(file.toString())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        try {
            val repository = OtpAccountRepository(database)
            val ids = (1..3).map { index ->
                repository.save(OtpAccountEntity(issuer = "Service $index", accountName = "user$index",
                    secret = "JBSWY3DPEHPK3PXP"))
            }
            database.preferenceDao().put(AppPreferenceEntity("theme", "dark"))

            assertEquals(0, repository.deleteAccounts(emptyList()))
            assertEquals(2, repository.deleteAccounts(listOf(ids[0], ids[2], ids[2], -1)))
            assertEquals(listOf(ids[1]), repository.observeAll().first().map { it.id })
            assertEquals("dark", database.preferenceDao().value("theme"))

            // 全选使用同一条批量删除路径，只包含用户确认的账户 ID。
            assertEquals(1, repository.deleteAccounts(repository.observeAll().first().map { it.id }))
            assertEquals(emptyList(), repository.observeAll().first())
            assertEquals(0, repository.deleteAccounts(ids))
            assertEquals("dark", database.preferenceDao().value("theme"))
        } finally {
            database.close()
            file.deleteIfExists()
        }
    }
}
