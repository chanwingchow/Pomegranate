package com.chow.pomegranate.repository

import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.AppPreferenceEntity
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class AppPreferenceRepository(database: AppDatabase) {
    private val dao = database.preferenceDao()

    fun observe(key: String): Flow<String?> = dao.observeValue(key)

    suspend fun get(key: String): String? = dao.value(key)

    suspend fun set(key: String, value: String?) {
        if (value == null) dao.remove(key) else dao.put(AppPreferenceEntity(key, value))
    }

    /** 删除 [key] 对应的偏好。 */
    suspend fun remove(key: String) {
        dao.remove(key)
    }
}
