package com.nafis.calories.domain.repository

import com.nafis.calories.domain.model.ConsumedTotals
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealSource
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    fun observeEntries(): Flow<List<MealEntry>>
    fun observeEntryCount(): Flow<Int>
    fun search(query: String): Flow<List<MealEntry>>

    suspend fun getScanResult(entryId: Long): ScanResult?
    suspend fun loadImage(entry: MealEntry): ByteArray?
    suspend fun getConsumedSince(startOfDay: Long): ConsumedTotals

    suspend fun saveScan(scanResult: ScanResult, jpegImage: ByteArray): Result<Long>
    suspend fun saveEntryWithoutImage(scanResult: ScanResult, source: MealSource, referenceId: String?): Result<Long>
    suspend fun saveCorrection(scanResult: ScanResult, jpegImage: ByteArray?): Result<Unit>
    suspend fun updateConsumedWeight(scanId: String, weightG: Double): Boolean
    suspend fun updateMealType(scanId: String, mealType: MealType): Boolean

    suspend fun delete(entryId: Long): Result<Unit>
    suspend fun deleteAll(): Result<Int>
}
