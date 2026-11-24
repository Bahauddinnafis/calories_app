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
    version = 10,
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

        // Versions 7 and 8 were version bumps without schema changes.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        /** Backend notes, suggested add-ons, and cloud sync bookkeeping. Existing rows start as pending. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN aiNoticeText TEXT")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN addOnsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN logSource TEXT")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN referenceId TEXT")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_history_v2 ADD COLUMN nutritionNote TEXT")
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    // Only versions older than 4 (development builds) have no migration path.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}