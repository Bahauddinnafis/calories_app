package com.nafis.calories.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Same schema history as the original Android app, so existing installs keep their data. */
internal object DatabaseMigrations {

    private const val TABLE = "scan_history_v2"

    private val MIGRATION_4_5 = migration(4, 5,
        "ALTER TABLE $TABLE ADD COLUMN consumedWeightG REAL",
        "ALTER TABLE $TABLE ADD COLUMN mealType TEXT"
    )

    private val MIGRATION_5_6 = migration(5, 6,
        "ALTER TABLE $TABLE ADD COLUMN isUserCorrected INTEGER NOT NULL DEFAULT 0"
    )

    private val MIGRATION_6_7 = migration(6, 7)

    private val MIGRATION_7_8 = migration(7, 8)

    private val MIGRATION_8_9 = migration(8, 9,
        "ALTER TABLE $TABLE ADD COLUMN aiNoticeText TEXT",
        "ALTER TABLE $TABLE ADD COLUMN addOnsJson TEXT NOT NULL DEFAULT '[]'",
        "ALTER TABLE $TABLE ADD COLUMN logSource TEXT",
        "ALTER TABLE $TABLE ADD COLUMN referenceId TEXT",
        "ALTER TABLE $TABLE ADD COLUMN remoteId TEXT",
        "ALTER TABLE $TABLE ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'",
        "ALTER TABLE $TABLE ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
    )

    private val MIGRATION_9_10 = migration(9, 10,
        "ALTER TABLE $TABLE ADD COLUMN nutritionNote TEXT"
    )

    private val MIGRATION_10_11 = migration(10, 11)

    val all: Array<Migration> = arrayOf(
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11
    )

    private fun migration(from: Int, to: Int, vararg statements: String): Migration =
        object : Migration(from, to) {
            override fun migrate(connection: SQLiteConnection) {
                statements.forEach(connection::execSQL)
            }
        }
}
