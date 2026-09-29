package com.chow.pomegranate.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * 用户实体。
 *
 * 只保存账号元数据。密码、Cookie 与第二课堂凭据均存放在平台安全存储中；保留
 * [secondClassroomPassword] 和 [cookies] 仅用于迁移旧版数据库，迁移后会清空。
 *
 * @property userId 学号
 * @property secondClassroomPassword 第二课堂密码
 * @property cookies 序列化后的 Cookie
 * @property isActive 是否为当前登录账号
 * @property savedAt 保存时间，用于列表排序
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val secondClassroomPassword: String = "",
    val cookies: String = "",
    val isActive: Boolean = false,
    val savedAt: Long = 0L,
)

/** 用户 DAO。 */
@Dao
interface UserDao {
    /** 观察全部已保存账号，当前账号优先。 */
    @Query("SELECT * FROM users ORDER BY isActive DESC, savedAt DESC")
    fun observeAll(): Flow<List<UserEntity>>

    /** 返回指定用户。 */
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun user(userId: String): UserEntity?

    /** 返回全部已保存账号。 */
    @Query("SELECT * FROM users ORDER BY isActive DESC, savedAt DESC")
    suspend fun users(): List<UserEntity>

    /** 返回当前登录账号。 */
    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    suspend fun activeUser(): UserEntity?

    /** 新增或更新用户。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(user: UserEntity)

    /** 将 [userId] 设为当前账号，同时取消其他账号的当前状态。 */
    @Query("UPDATE users SET isActive = (userId = :userId)")
    suspend fun setActive(userId: String)

    /** 删除用户。 */
    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun remove(userId: String)
}
