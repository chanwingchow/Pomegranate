package com.chow.pomegranate.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.mp.KoinPlatform

actual fun platformDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = KoinPlatform.getKoin().get<Context>().applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = context.getDatabasePath("pomegranate.db").absolutePath,
    )
}
