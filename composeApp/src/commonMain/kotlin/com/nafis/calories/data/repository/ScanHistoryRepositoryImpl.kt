package com.nafis.calories.data.repository

import com.nafis.calories.core.result.suspendRunCatching
import com.nafis.calories.data.local.dao.ScanHistoryDao
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.local.image.ImageStore
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.data.sync.PendingDeletionStore
import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.domain.model.ConsumedTotals
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealSource
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ScanHistoryRepositoryImpl(
    private val dao: ScanHistoryDao,
    private val imageStore: ImageStore,
    private val pendingDeletions: PendingDeletionStore,
    private val syncTrigger: SyncTrigger,
    private val clock: Clock
) : ScanHistoryRepository {

    override fun observeEntries(): Flow<List<MealEntry>> = dao.getAllHistories().toEntries()

    override fun observeEntryCount(): Flow<Int> = dao.getHistoryCount()

    override fun search(query: String): Flow<List<MealEntry>> = dao.searchHistories(query).toEntries()

    override suspend fun getScanResult(entryId: Long): ScanResult? =
        dao.getHistoryById(entryId)?.let(HistoryMapper::toDomain)

    override suspend fun loadImage(entry: MealEntry): ByteArray? =
        entry.imagePath?.let { imageStore.load(it) }

    override suspend fun getConsumedSince(startOfDay: Long): ConsumedTotals {
        val rows = dao.getHistoriesSince(startOfDay)
        return ConsumedTotals(
            energyKcal = rows.sumOf { it.consumedEnergyKcal ?: 0.0 },
            proteinG = rows.sumOf { it.consumedProteinG ?: 0.0 }
        )
    }

    override suspend fun saveScan(scanResult: ScanResult, jpegImage: ByteArray): Result<Long> {
        val imagePath = imageStore.save(jpegImage)
            ?: return Result.failure(IllegalStateException("Failed to save image"))
        return insert(toEntity(scanResult, imagePath, MealSource.SCAN, scanResult.scanId))
            .onFailure { imageStore.delete(imagePath) }
    }

    override suspend fun saveEntryWithoutImage(
        scanResult: ScanResult,
        source: MealSource,
        referenceId: String?
    ): Result<Long> = insert(toEntity(scanResult, imagePath = "", source, referenceId))

    override suspend fun saveCorrection(scanResult: ScanResult, jpegImage: ByteArray?): Result<Unit> {
        val existing = dao.getHistoryByScanId(scanResult.scanId)
            ?: return when (jpegImage) {
                null -> Result.failure(IllegalStateException("No history row or image for scan ${scanResult.scanId}"))
                else -> saveScan(scanResult, jpegImage).map { }
            }

        return suspendRunCatching {
            val source = MealSource.fromKey(existing.resolvedLogSource) ?: MealSource.SCAN
            val corrected = toEntity(scanResult, existing.imagePath, source, existing.referenceId ?: existing.scanId)
                .copy(
                    id = existing.id,
                    timestamp = existing.timestamp,
                    mealType = scanResult.mealType?.key ?: existing.mealType,
                    remoteId = existing.remoteId
                )
            dao.update(corrected)
            syncTrigger.requestSync()
        }
    }

    override suspend fun updateConsumedWeight(scanId: String, weightG: Double): Boolean =
        (dao.updateConsumedWeight(scanId, weightG, now()) > 0).also { if (it) syncTrigger.requestSync() }

    override suspend fun updateMealType(scanId: String, mealType: MealType): Boolean =
        (dao.updateMealType(scanId, mealType.key, now()) > 0).also { if (it) syncTrigger.requestSync() }

    override suspend fun delete(entryId: Long): Result<Unit> = suspendRunCatching {
        val entity = dao.getHistoryById(entryId) ?: return@suspendRunCatching
        dao.delete(entity)
        if (entity.hasImage) imageStore.delete(entity.imagePath)
        entity.remoteId?.let { pendingDeletions.add(listOf(it)) }
        syncTrigger.requestSync()
    }

    override suspend fun deleteAll(): Result<Int> = suspendRunCatching {
        pendingDeletions.add(dao.getAllRemoteIds())
        val deleted = dao.deleteAll()
        imageStore.deleteAll()
        syncTrigger.requestSync()
        deleted
    }

    private suspend fun insert(entity: ScanHistoryEntity): Result<Long> = suspendRunCatching {
        dao.insert(entity).also { syncTrigger.requestSync() }
    }

    private fun toEntity(
        scanResult: ScanResult,
        imagePath: String,
        source: MealSource,
        referenceId: String?
    ): ScanHistoryEntity = HistoryMapper.toEntity(scanResult, imagePath, source.key, referenceId, updatedAt = now())

    private fun Flow<List<ScanHistoryEntity>>.toEntries(): Flow<List<MealEntry>> =
        map { rows -> rows.map(HistoryMapper::toEntry) }

    private fun now(): Long = clock.now().toEpochMilliseconds()
}