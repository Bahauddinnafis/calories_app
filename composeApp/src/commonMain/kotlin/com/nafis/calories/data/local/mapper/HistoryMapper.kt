package com.nafis.calories.data.local.mapper

import com.nafis.calories.core.serialization.AppJson
import com.nafis.calories.data.local.entity.IngredientSnapshot
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.domain.model.Ingredient
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealSource
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.Nutrition
import com.nafis.calories.domain.model.NutritionSource
import com.nafis.calories.domain.model.ScanNotes
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.model.ScanStatus
import com.nafis.calories.domain.model.ServingNormalizer
import com.nafis.calories.domain.model.SuggestedAddOn

object HistoryMapper {

    private val json = AppJson

    fun toEntity(
        scanResult: ScanResult,
        imagePath: String,
        source: String,
        referenceId: String?,
        updatedAt: Long
    ): ScanHistoryEntity {
        val totals = scanResult.totalNutrition
        return ScanHistoryEntity(
            scanId = scanResult.scanId,
            mealName = scanResult.mealName,
            servingWeightG = scanResult.servingWeightG,
            singleServingWeightG = scanResult.singleServingWeightG,
            servingUnitName = scanResult.servingUnitName,
            servingUnitCount = scanResult.servingUnitCount,
            servingConfidence = scanResult.servingConfidence,
            ingredientsJson = json.encodeToString(scanResult.ingredients.map(::toSnapshot)),
            totalEnergyKcal = totals.energyKcal,
            totalProteinG = totals.proteinG,
            totalFatG = totals.fatG,
            totalCarbohydrateG = totals.carbohydrateG,
            totalSugarG = totals.sugarG,
            totalSaturatedFatG = totals.saturatedFatG,
            totalCholesterolMg = totals.cholesterolMg,
            totalFiberG = totals.fiberG,
            totalSodiumMg = totals.sodiumMg,
            totalCalciumMg = totals.calciumMg,
            totalIronMg = totals.ironMg,
            isComplete = scanResult.isComplete,
            missingNutrientsJson = json.encodeToString(scanResult.missingNutrients),
            overallConfidence = scanResult.overallConfidence,
            uncertaintyNotesJson = json.encodeToString(scanResult.uncertaintyNotes),
            imagePath = imagePath,
            timestamp = scanResult.timestamp,
            consumedWeightG = scanResult.consumedWeightG ?: scanResult.defaultConsumedWeightG,
            mealType = scanResult.mealType?.key,
            isUserCorrected = scanResult.isUserCorrected,
            aiNoticeText = scanResult.aiDisclaimerText,
            nutritionNote = scanResult.personalNutritionNote,
            addOnsJson = json.encodeToString(scanResult.suggestedAddOns),
            logSource = source,
            referenceId = referenceId,
            syncStatus = ScanHistoryEntity.SYNC_PENDING,
            updatedAt = updatedAt
        )
    }

    private fun toSnapshot(ingredient: Ingredient) = IngredientSnapshot(
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

    fun toDomain(entity: ScanHistoryEntity): ScanResult {
        val serving = ServingNormalizer.normalize(
            totalWeightG = entity.servingWeightG,
            singleServingWeightG = entity.singleServingWeightG,
            unitName = entity.servingUnitName,
            unitCount = entity.servingUnitCount
        )
        val ingredients = parseIngredients(entity.ingredientsJson)
        val uncertaintyNotes = parseStringList(entity.uncertaintyNotesJson)
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
            uncertaintyNotes = uncertaintyNotes,
            timestamp = entity.timestamp,
            consumedWeightG = entity.consumedWeightG,
            mealType = mealTypeOf(entity),
            isUserCorrected = entity.isUserCorrected,
            personalNutritionNote = ScanNotes.personalNote(entity.nutritionNote, uncertaintyNotes),
            aiDisclaimerText = if (entity.resolvedLogSource == ScanHistoryEntity.SOURCE_SCAN) {
                ScanNotes.aiDisclaimer(entity.aiNoticeText)
            } else {
                null
            },
            suggestedAddOns = parseAddOns(entity.addOnsJson)
        )
    }

    fun toEntry(entity: ScanHistoryEntity) = MealEntry(
        id = entity.id,
        scanId = entity.scanId,
        mealName = entity.mealName,
        timestamp = entity.timestamp,
        mealType = mealTypeOf(entity),
        source = MealSource.fromKey(entity.resolvedLogSource) ?: MealSource.SCAN,
        imagePath = entity.imagePath.takeIf { it.isNotBlank() },
        consumedWeightG = entity.consumedWeightG ?: entity.servingWeightG,
        totalEnergyKcal = entity.totalEnergyKcal,
        energyKcal = entity.consumedEnergyKcal,
        proteinG = entity.consumedProteinG,
        carbohydrateG = entity.consumedCarbohydrateG,
        fatG = entity.consumedFatG
    )

    fun mealTypeOf(entity: ScanHistoryEntity): MealType {
        MealType.fromKey(entity.mealType)?.let { return it }
        val hour = ((entity.timestamp / (1000 * 60 * 60)) % 24).toInt().let { if (it < 0) it + 24 else it }
        return MealType.fromHour(hour)
    }

    private fun parseIngredients(value: String): List<Ingredient> {
        return try {
            if (value.isBlank() || value == "[]") return emptyList()
            val snapshots: List<IngredientSnapshot> = json.decodeFromString(value)
            snapshots.map { snapshot ->
                Ingredient(
                    foodCode = snapshot.foodCode,
                    foodName = snapshot.foodName,
                    estimatedWeightG = snapshot.estimatedWeightG,
                    matchStatus = snapshot.matchStatus,
                    matchConfidence = snapshot.matchConfidence,
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
            emptyList()
        }
    }

    private fun parseAddOns(value: String): List<SuggestedAddOn> {
        return try {
            if (value.isBlank() || value == "[]") return emptyList()
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseStringList(value: String): List<String> {
        return try {
            if (value.isBlank() || value == "[]") return emptyList()
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}