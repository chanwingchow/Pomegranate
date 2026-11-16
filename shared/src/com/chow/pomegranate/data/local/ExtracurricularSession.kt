package com.chow.pomegranate.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query

/**
 * 第二课堂会话实体。
 *
 * 第二课堂的接口依赖登录返回的会话 ID，重启后需要恢复，否则包含 `!!` 的取用会直接崩溃。
 *
 * @property userId 学号
 * @property sessionId 会话 ID
 * @property password 密码，用于会话失效后自动重新登录
 */
@Entity(tableName = "extracurricular_sessions")
data class ExtracurricularSessionEntity(
    @PrimaryKey val userId: String,
    val sessionId: String,
    val password: String = "",
)

/** 第二课堂会话 DAO。 */
@Dao
interface ExtracurricularSessionDao {
    /** 返回 [userId] 的会话。 */
    @Query("SELECT * FROM extracurricular_sessions WHERE userId = :userId LIMIT 1")
    suspend fun session(userId: String): ExtracurricularSessionEntity?

    /** 新增或更新会话。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: ExtracurricularSessionEntity)

    /** 删除会话。 */
    @Query("DELETE FROM extracurricular_sessions WHERE userId = :userId")
    suspend fun remove(userId: String)
}
