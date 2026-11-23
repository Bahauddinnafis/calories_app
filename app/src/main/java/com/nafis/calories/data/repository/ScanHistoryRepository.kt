package com.nafis.calories.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.nafis.calories.data.local.AppDatabase
import com.nafis.calories.data.local.entity.IngredientSnapshot
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.utils.ImageStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing scan history.
 * Handles mapping between ScanResult domain model and ScanHistoryEntity.
 * Preserves immutable snapshots of scan results.
 */
class ScanHistoryRepository(private val context: Context) {

    private val scanHistoryDao by lazy {
        AppDatabase.getDatabase(context).scanHistoryDao()
    }

    private val gson = Gson()

    companion object {
        private const val TAG = "CaloriesPipeline"
    }

    suspend fun saveScanHistory(
        scanResult: ScanResult,
        capturedImage: Bitmap
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "[History] Saving scan to history...")
            Log.d(TAG, "[History]   Meal: ${scanResult.mealName}")
            Log.d(TAG, "[History]   Ingredients: ${scanResult.ingredients.size}")
            Log.d(TAG, "[History]   Calories: ${scanResult.totalNutrition.energyKcal?.toInt() ?: "?"} kkal")

            // Step 1: Save image to internal storage
            val imagePath = ImageStorageUtil.saveImage(context, capturedImage)
                ?: return@withContext Result.failure(Exception("Failed to save image"))

            Log.d(TAG, "[History]   Image saved: $imagePath")

            val history = toEntity(scanResult, imagePath)

            // Insert to database
            try {
                val id = scanHistoryDao.insert(history)
                Log.d(TAG, "[History] ✓ Saved successfully (ID: $id)")
                Result.success(id)
            } catch (e: Exception) {
                // Rollback: Delete saved image if DB insert fails
                ImageStorageUtil.deleteImage(context, imagePath)
                Log.e(TAG, "[History] ✗ Database insert failed", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[History] ✗ Save failed", e)
            Result.failure(e)
        }
    }

    suspend fun saveCorrection(scanResult: ScanResult, image: Bitmap?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = scanHistoryDao.getHistoryByScanId(scanResult.scanId)
            if (existing != null) {
                val corrected = toEntity(scanResult, existing.imagePath).copy(
                    id = existing.id,
                    timestamp = existing.timestamp,
                    mealType = scanResult.mealType?.key ?: existing.mealType
                )
                scanHistoryDao.update(corrected)
                Log.d(TAG, "[History] ✓ Correction saved (ID: ${existing.id})")
                Result.success(Unit)
            } else if (image != null) {
                saveScanHistory(scanResult, image).map { }
            } else {
                Result.failure(IllegalStateException("No history row or image for scan ${scanResult.scanId}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[History] ✗ Saving correction failed", e)
            Result.failure(e)
        }
    }

    private fun toEntity(scanResult: ScanResult, imagePath: String): ScanHistoryEntity {
        val ingredientSnapshots = scanResult.ingredients.map { ingredient ->
            IngredientSnapshot(
                foodCode = ingredient.foodCode,
                foodName = ingredient.foodName,
                estimatedWeightG = ingredient.estimatedWeightG,
                matchStatus = ingredient.matchStatus,
                matchConfidence = ingredient.matchConfidence,
                energyKcal = ingredient.nutrition?.energyKcal,
                proteinG = ingredient.nutrition?.proteinG,
                fatG = ingredient.nutrition?.fatG,
                carbohydrateG = ingredient.nutrition?.carbohydrateG,
                sugarG = ingredient.nutrition?.sugarG,
                saturatedFatG = ingredient.nutrition?.saturatedFatG,
                cholesterolMg = ingredient.nutrition?.cholesterolMg,
                fiberG = ingredient.nutrition?.fiberG,
                sodiumMg = ingredient.nutrition?.sodiumMg,
                calciumMg = ingredient.nutrition?.calciumMg,
                ironMg = ingredient.nutrition?.ironMg,
                sourceName = ingredient.source?.sourceName,
                sourceVersion = ingredient.source?.sourceVersion
            )
        }

        return ScanHistoryEntity(
            scanId = scanResult.scanId,
            mealName = scanResult.mealName,
            servingWeightG = scanResult.servingWeightG,
            singleServingWeightG = scanResult.singleServingWeightG,
            servingUnitName = scanResult.servingUnitName,
            servingUnitCount = scanResult.servingUnitCount,
            servingConfidence = scanResult.servingConfidence,
            ingredientsJson = gson.toJson(ingredientSnapshots),
            totalEnergyKcal = scanResult.totalNutrition.energyKcal,
            totalProteinG = scanResult.totalNutrition.proteinG,
            totalFatG = scanResult.totalNutrition.fatG,
            totalCarbohydrateG = scanResult.totalNutrition.carbohydrateG,
            totalSugarG = scanResult.totalNutrition.sugarG,
            totalSaturatedFatG = scanResult.totalNutrition.saturatedFatG,
            totalCholesterolMg = scanResult.totalNutrition.cholesterolMg,
            totalFiberG = scanResult.totalNutrition.fiberG,
            totalSodiumMg = scanResult.totalNutrition.sodiumMg,
            totalCalciumMg = scanResult.totalNutrition.calciumMg,
            totalIronMg = scanResult.totalNutrition.ironMg,
            isComplete = scanResult.isComplete,
            missingNutrientsJson = gson.toJson(scanResult.missingNutrients),
            overallConfidence = scanResult.overallConfidence,
            uncertaintyNotesJson = gson.toJson(scanResult.uncertaintyNotes),
            imagePath = imagePath,
            timestamp = scanResult.timestamp,
            consumedWeightG = scanResult.consumedWeightG ?: scanResult.defaultConsumedWeightG,
            mealType = scanResult.mealType?.key,
            isUserCorrected = scanResult.isUserCorrected
        )
    }

    suspend fun updateConsumedWeight(scanId: String, weightG: Double): Boolean = withContext(Dispatchers.IO) {
        scanHistoryDao.updateConsumedWeight(scanId, weightG) > 0
    }

    suspend fun updateMealType(scanId: String, mealType: MealType): Boolean = withContext(Dispatchers.IO) {
        scanHistoryDao.updateMealType(scanId, mealType.key) > 0
    }

    /**
     * Get all scan histories, ordered by most recent first.
     */
    fun getAllHistories(): Flow<List<ScanHistoryEntity>> {
        return scanHistoryDao.getAllHistories()
    }

    /**
     * Get scan history by ID.
     */
    suspend fun getHistoryById(id: Long): ScanHistoryEntity? = withContext(Dispatchers.IO) {
        scanHistoryDao.getHistoryById(id)
    }

    /**
     * Delete single history entry.
     */
    suspend fun deleteHistory(history: ScanHistoryEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Delete from database
            scanHistoryDao.delete(history)

            // Step 2: Delete associated image (best effort)
            ImageStorageUtil.deleteImage(context, history.imagePath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all history entries.
     */
    suspend fun deleteAllHistories(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Delete all from database
            val deletedCount = scanHistoryDao.deleteAll()

            // Step 2: Delete all images (best effort)
            ImageStorageUtil.deleteAllImages(context)

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search histories by meal name.
     */
    fun searchHistories(query: String): Flow<List<ScanHistoryEntity>> {
        return scanHistoryDao.searchHistories(query)
    }

    /**
     * Get total history count.
     */
    fun getHistoryCount(): Flow<Int> {
        return scanHistoryDao.getHistoryCount()
    }

    /**
     * Load history image from storage.
     */
    suspend fun loadHistoryImage(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val bitmap = ImageStorageUtil.loadImage(context, imagePath)
        if (bitmap != null) {
            Log.d(TAG, "[History] Image loaded: $imagePath (${bitmap.width}x${bitmap.height})")
        } else {
            Log.w(TAG, "[History] Image not found: $imagePath")
        }
        bitmap
    }
}