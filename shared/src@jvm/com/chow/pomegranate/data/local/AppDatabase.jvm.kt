package com.chow.pomegranate.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun platformDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val directory = File(System.getProperty("user.home"), ".pomegranate").apply { mkdirs() }
    return Room.databaseBuilder<AppDatabase>(File(directory, "pomegranate.db").absolutePath)
}
