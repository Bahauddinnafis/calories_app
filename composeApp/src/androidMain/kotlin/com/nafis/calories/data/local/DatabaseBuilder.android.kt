package com.nafis.calories.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(AppDatabase.NAME).absolutePath
    )
}
