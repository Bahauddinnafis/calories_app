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

    @Query("SELECT * FROM scan_history_v2 WHERE timestamp >= :since")
    suspend fun getHistoriesSince(since: Long): List<ScanHistoryEntity>

    @Query(
        "UPDATE scan_history_v2 SET consumedWeightG = :weightG, syncStatus = 'pending', updatedAt = :now " +
            "WHERE scanId = :scanId"
    )
    suspend fun updateConsumedWeight(scanId: String, weightG: Double, now: Long = System.currentTimeMillis()): Int

    @Query(
        "UPDATE scan_history_v2 SET mealType = :mealType, syncStatus = 'pending', updatedAt = :now " +
            "WHERE scanId = :scanId"
    )
    suspend fun updateMealType(scanId: String, mealType: String, now: Long = System.currentTimeMillis()): Int

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

    // --- Cloud sync ---

    @Query("SELECT * FROM scan_history_v2 WHERE syncStatus = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingSync(): List<ScanHistoryEntity>

    @Query("SELECT remoteId FROM scan_history_v2 WHERE remoteId IS NOT NULL")
    suspend fun getAllRemoteIds(): List<String>

    /** Marks a row synced only if it wasn't edited while the upload was in flight. */
    @Query(
        "UPDATE scan_history_v2 SET syncStatus = 'synced', remoteId = :remoteId " +
            "WHERE id = :id AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(id: Long, remoteId: String, updatedAt: Long): Int

    /** Stores the backend ID even when the row changed meanwhile, so the next pass PATCHes it. */
    @Query("UPDATE scan_history_v2 SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String): Int

    /** After switching to a new backend account: forget remote IDs and re-upload every entry. */
    @Query("UPDATE scan_history_v2 SET remoteId = NULL, syncStatus = 'pending'")
    suspend fun resetSyncState(): Int
}