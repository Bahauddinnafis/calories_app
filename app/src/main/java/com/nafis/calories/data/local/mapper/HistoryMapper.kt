package com.nafis.calories.data.local.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nafis.calories.data.local.entity.IngredientSnapshot
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.model.Ingredient
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.model.Nutrition
import com.nafis.calories.data.model.NutritionSource
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.data.model.ScanStatus
import com.nafis.calories.data.model.ServingNormalizer
import java.util.Calendar

/**
 * Mapper to convert ScanHistoryEntity back to ScanResult domain model.
 * Handles JSON deserialization and graceful fallback for older records.
 */
object HistoryMapper {

    private val gson = Gson()

    fun toDomain(entity: ScanHistoryEntity): ScanResult {
        // Older records may hold inconsistent serving data saved before validation existed.
        val serving = ServingNormalizer.normalize(
            totalWeightG = entity.servingWeightG,
            singleServingWeightG = entity.singleServingWeightG,
            unitName = entity.servingUnitName,
            unitCount = entity.servingUnitCount
        )
        val ingredients = parseIngredients(entity.ingredientsJson)
        val totals = Nutrition(
            energyKcal = entity.totalEnergyKcal,
            proteinG = entity.totalProteinG,
            fatG = entity.totalFatG,
            carbohydrateG = entity.totalCarbohydrateG,
            sugarG = entity.totalSugarG,
            saturatedFatG = entity.totalSaturatedFatG,
            cholesterolMg = entity.totalCholesterolMg,
            fiberG = entity.totalFiberG,
            sodiumMg = entity.totalSodiumMg,
            calciumMg = entity.totalCalciumMg,
            ironMg = entity.totalIronMg
        )

        return ScanResult(
            scanId = entity.scanId,
            mealName = entity.mealName,
            servingWeightG = serving.totalWeightG,
            singleServingWeightG = serving.singleServingWeightG,
            servingUnitName = serving.unitName,
            servingUnitCount = serving.unitCount,
            servingConfidence = entity.servingConfidence,
            ingredients = ingredients,
            totalNutrition = totals,
            status = ScanStatus.resolve(null, ingredients, totals, entity.isComplete),
            isComplete = entity.isComplete,
            missingNutrients = parseStringList(entity.missingNutrientsJson),
            overallConfidence = entity.overallConfidence,
            uncertaintyNotes = parseStringList(entity.uncertaintyNotesJson),
            timestamp = entity.timestamp,
            consumedWeightG = entity.consumedWeightG,
            mealType = mealTypeOf(entity),
            isUserCorrected = entity.isUserCorrected
        )
    }

    /** Stored meal type, or one suggested from the scan time for rows saved before it existed. */
    fun mealTypeOf(entity: ScanHistoryEntity): MealType {
        MealType.fromKey(entity.mealType)?.let { return it }
        val hour = Calendar.getInstance().apply { timeInMillis = entity.timestamp }.get(Calendar.HOUR_OF_DAY)
        return MealType.fromHour(hour)
    }

    private fun parseIngredients(json: String): List<Ingredient> {
        return try {
            if (json.isBlank() || json == "[]") {
                return emptyList()
            }

            val type = object : TypeToken<List<IngredientSnapshot>>() {}.type
            val snapshots: List<IngredientSnapshot> = gson.fromJson(json, type)

            snapshots.map { snapshot ->
                Ingredient(
                    foodCode = snapshot.foodCode,
                    foodName = snapshot.foodName,
                    estimatedWeightG = snapshot.estimatedWeightG,
                    matchStatus = snapshot.matchStatus,
                    matchConfidence = snapshot.matchConfidence,
                    // Snapshots flatten nutrition; an all-null set means "no nutrition".
                    nutrition = Nutrition(
                        energyKcal = snapshot.energyKcal,
                        proteinG = snapshot.proteinG,
                        fatG = snapshot.fatG,
                        carbohydrateG = snapshot.carbohydrateG,
                        sugarG = snapshot.sugarG,
                        saturatedFatG = snapshot.saturatedFatG,
                        cholesterolMg = snapshot.cholesterolMg,
                        fiberG = snapshot.fiberG,
                        sodiumMg = snapshot.sodiumMg,
                        calciumMg = snapshot.calciumMg,
                        ironMg = snapshot.ironMg
                    ).takeIf { it.hasAnyValue() },
                    source = if (snapshot.sourceName != null) {
                        NutritionSource(
                            sourceName = snapshot.sourceName,
                            sourceVersion = snapshot.sourceVersion ?: ""
                        )
                    } else {
                        null
                    }
                )
            }
        } catch (e: Exception) {
            // Graceful fallback for corrupted or old format JSON
            android.util.Log.w("HistoryMapper", "Failed to parse ingredients JSON: ${e.message}")
            emptyList()
        }
    }

    private fun parseStringList(json: String): List<String> {
        return try {
            if (json.isBlank() || json == "[]") {
                return emptyList()
            }

            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            // Graceful fallback for corrupted JSON
            android.util.Log.w("HistoryMapper", "Failed to parse string list JSON: ${e.message}")
            emptyList()
        }
    }
}