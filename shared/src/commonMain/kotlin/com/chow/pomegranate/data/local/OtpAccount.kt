package com.chow.pomegranate.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** OTP 账户实体。 */
@Entity(tableName = "otp_accounts")
data class OtpAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String,
    val accountName: String,
    val secret: String,
    val digits: Int = 6,
    val periodSeconds: Long = 30,
)

/** OTP 账户 DAO。 */
@Dao
interface OtpAccountDao {
    /** 按名称观察全部账户。 */
    @Query("SELECT * FROM otp_accounts ORDER BY issuer COLLATE NOCASE, accountName COLLATE NOCASE, id")
    fun observeAll(): Flow<List<OtpAccountEntity>>

    /** 返回指定账户。 */
    @Query("SELECT * FROM otp_accounts WHERE id = :id LIMIT 1")
    suspend fun account(id: Long): OtpAccountEntity?

    /** 新增或更新账户。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(account: OtpAccountEntity): Long

    /** 删除账户。 */
    @Delete
    suspend fun delete(account: OtpAccountEntity)
}
