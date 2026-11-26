package com.nafis.calories.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nafis.calories.data.local.dao.ScanHistoryDao
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [ScanHistoryEntity::class],
    version = 11,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        const val NAME = "calories_database"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(): AppDatabase =
    addMigrations(*DatabaseMigrations.all)
        .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
