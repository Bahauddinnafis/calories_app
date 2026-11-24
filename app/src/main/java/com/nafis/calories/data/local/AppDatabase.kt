package com.nafis.calories.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nafis.calories.data.local.dao.ScanHistoryDao
import com.nafis.calories.data.local.entity.ScanHistoryEntity

@Database(
    entities = [ScanHistoryEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN consumedWeightG REAL")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN mealType TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN isUserCorrected INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            // Fast path: Return existing instance
            return INSTANCE ?: synchronized(this) {
                // Double-check inside synchronized block
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calories_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    // Only versions older than 4 (development builds) have no migration path.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}