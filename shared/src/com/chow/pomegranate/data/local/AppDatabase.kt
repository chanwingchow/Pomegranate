package com.chow.pomegranate.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** 应用数据库。 */
@Database(
    entities = [
        AppPreferenceEntity::class,
        OtpAccountEntity::class,
        UserEntity::class,
        ExtracurricularSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    /** 返回应用偏好 DAO。 */
    abstract fun preferenceDao(): AppPreferenceDao

    /** 返回 OTP 账户 DAO。 */
    abstract fun otpAccountDao(): OtpAccountDao

    /** 返回用户 DAO。 */
    abstract fun userDao(): UserDao

    /** 返回第二课堂会话 DAO。 */
    abstract fun extracurricularSessionDao(): ExtracurricularSessionDao
}

/** Room 生成的数据库构造器。 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** 返回应用数据库。 */
fun createAppDatabase(): AppDatabase = platformDatabaseBuilder()
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

/** 返回当前平台的数据库构建器。 */
expect fun platformDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
