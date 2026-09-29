package com.chow.pomegranate.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** 应用偏好实体。 */
@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** 应用偏好 DAO。 */
@Dao
interface AppPreferenceDao {
    /** 观察 [key] 对应的值。 */
    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>

    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun value(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(value: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun remove(key: String)
}
    /** 返回 [key] 对应的值。 */
    /** 写入偏好。 */
    /** 删除 [key] 对应的偏好。 */
