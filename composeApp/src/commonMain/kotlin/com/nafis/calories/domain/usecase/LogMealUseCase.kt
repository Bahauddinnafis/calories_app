package com.nafis.calories.domain.usecase

import com.nafis.calories.domain.model.MealRecommendation
import com.nafis.calories.domain.model.MealSource
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.Nutrition
import com.nafis.calories.domain.model.PackagedProduct
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.repository.ScanHistoryRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Logs one serving of a meal that has no photo: a recommendation, a packaged product, or a manual entry. */
class LogMealUseCase(
    private val history: ScanHistoryRepository,
    private val clock: Clock
) {

    suspend fun logRecommendation(meal: MealRecommendation): Result<Long> = log(
        idPrefix = "rec_${meal.id}",
        source = MealSource.RECOMMENDATION,
        referenceId = meal.id,
        mealName = meal.title,
        nutrition = nutrition(meal.energyKcal, meal.proteinG, meal.carbohydrateG, meal.fatG),
        servingWeightG = meal.servingWeightG
    )

    suspend fun logProduct(product: PackagedProduct): Result<Long> {
        val name = product.brand?.takeIf { !product.name.contains(it, ignoreCase = true) }
            ?.let { "${product.name} ($it)" } ?: product.name
        return log(
            idPrefix = "product",
            source = MealSource.PRODUCT,
            referenceId = product.id,
            mealName = name,
            nutrition = nutrition(product.energyKcal, product.proteinG, product.carbohydrateG, product.fatG),
            servingWeightG = product.servingWeightG
        )
    }

    suspend fun logManual(
        mealName: String,
        energyKcal: Double,
        proteinG: Double,
        carbohydrateG: Double,
        fatG: Double,
        servingWeightG: Double
    ): Result<Long> = log(
        idPrefix = "manual",
        source = MealSource.MANUAL,
        referenceId = null,
        mealName = mealName,
        nutrition = nutrition(energyKcal, proteinG, carbohydrateG, fatG),
        servingWeightG = servingWeightG
    )

    /** Without a known weight the entry is one serving: a unit of 1 keeps the eaten ratio at 100%. */
    private suspend fun log(
        idPrefix: String,
        source: MealSource,
        referenceId: String?,
        mealName: String,
        nutrition: Nutrition,
        servingWeightG: Double
    ): Result<Long> {
        val now = clock.now()
        val servingWeight = servingWeightG.takeIf { it > 0.0 } ?: 1.0
        val scanResult = ScanResult(
            scanId = "${idPrefix}_${now.toEpochMilliseconds()}",
            mealName = mealName,
            servingWeightG = servingWeight,
            servingConfidence = 1.0,
            ingredients = emptyList(),
            totalNutrition = nutrition,
            isComplete = true,
            missingNutrients = emptyList(),
            overallConfidence = 1.0,
            uncertaintyNotes = emptyList(),
            timestamp = now.toEpochMilliseconds(),
            consumedWeightG = servingWeight,
            mealType = MealType.fromHour(now.toLocalDateTime(TimeZone.currentSystemDefault()).hour)
        )
        return history.saveEntryWithoutImage(scanResult, source, referenceId)
    }

    private fun nutrition(energyKcal: Double, proteinG: Double, carbohydrateG: Double, fatG: Double) = Nutrition(
        energyKcal = energyKcal,
        proteinG = proteinG,
        fatG = fatG,
        carbohydrateG = carbohydrateG,
        sugarG = null,
        saturatedFatG = null,
        cholesterolMg = null,
        fiberG = null,
        sodiumMg = null,
        calciumMg = null,
        ironMg = null
    )
}