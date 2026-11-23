package com.nafis.calories.data.local.dao

import androidx.room.*
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for new scan_history_v2 table with immutable snapshots.
 * Uses separate table name to avoid conflicts with legacy scan_history.
 */
@Dao
interface ScanHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ScanHistoryEntity): Long

    @Query("SELECT * FROM scan_history_v2 ORDER BY timestamp DESC")
    fun getAllHistories(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history_v2 WHERE id = :historyId")
    suspend fun getHistoryById(historyId: Long): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history_v2 WHERE scanId = :scanId LIMIT 1")
    suspend fun getHistoryByScanId(scanId: String): ScanHistoryEntity?

    @Query("UPDATE scan_history_v2 SET consumedWeightG = :weightG WHERE scanId = :scanId")
    suspend fun updateConsumedWeight(scanId: String, weightG: Double): Int

    @Query("UPDATE scan_history_v2 SET mealType = :mealType WHERE scanId = :scanId")
    suspend fun updateMealType(scanId: String, mealType: String): Int

    @Update
    suspend fun update(history: ScanHistoryEntity)

    @Delete
    suspend fun delete(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history_v2")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM scan_history_v2")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT * FROM scan_history_v2 WHERE mealName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistories(query: String): Flow<List<ScanHistoryEntity>>
}