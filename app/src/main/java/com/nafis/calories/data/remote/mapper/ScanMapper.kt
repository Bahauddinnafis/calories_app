package com.nafis.calories.data.remote.mapper

import com.nafis.calories.data.model.Ingredient
import com.nafis.calories.data.model.Nutrition
import com.nafis.calories.data.model.NutritionSource
import com.nafis.calories.data.model.ScanError
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.data.model.ScanStatus
import com.nafis.calories.data.model.ServingNormalizer
import com.nafis.calories.data.remote.dto.*

/** Mapper object to convert backend DTOs to domain models safely. */
object ScanMapper {

    fun toDomain(dto: ScanResponseDto): ScanResult {
        val prov = dto.nutritionProvenance
        val aiNotice = prov?.aiNotice?.id ?: prov?.aiNotice?.en
        val ingredients = dto.ingredients.orEmpty().map { it.toDomain() }
        val totals = dto.totals.toDomain()
        val totalsComplete = dto.totals.complete

        val serving = ServingNormalizer.normalize(
            totalWeightG = dto.servingEstimate.totalWeightG,
            singleServingWeightG = dto.servingEstimate.singleServingWeightG,
            unitName = dto.servingEstimate.servingUnitName,
            unitCount = dto.servingEstimate.servingUnitCount
        )

        return ScanResult(
            scanId = dto.scanId,
            mealName = dto.mealName,
            servingWeightG = serving.totalWeightG,
            singleServingWeightG = serving.singleServingWeightG,
            servingUnitName = serving.unitName,
            servingUnitCount = serving.unitCount,
            servingConfidence = dto.servingEstimate.confidence,
            ingredients = ingredients,
            totalNutrition = totals,
            isComplete = totalsComplete,
            missingNutrients = dto.totals.missingNutrients.orEmpty(),
            overallConfidence = dto.analysis.overallConfidence,
            isUserCorrected = dto.analysis.isUserCorrected == true,
            uncertaintyNotes = dto.analysis.uncertaintyNotes.orEmpty(),
            displayAiNotice = prov?.displayAiNotice ?: false,
            aiNoticeText = aiNotice,
            status = ScanStatus.resolve(dto.status, ingredients, totals, totalsComplete),
            errors = dto.errors.orEmpty().map { it.toDomain() }
        )
    }

    private fun ScanErrorDto.toDomain(): ScanError {
        return ScanError(
            stage = stage,
            code = code,
            message = message,
            retryable = retryable ?: false
        )
    }

    private fun IngredientDto.toDomain(): Ingredient {
        return Ingredient(
            foodCode = this.foodCode,
            foodName = this.foodName,
            estimatedWeightG = this.estimatedWeightG,
            matchStatus = this.matchStatus,
            matchConfidence = this.matchConfidence,
            nutritionSourceType = this.nutritionSourceType,
            nutritionStatus = this.nutritionStatus,
            nutrition = this.nutrition?.toDomain(),
            source = this.source?.toDomain()
        )
    }

    private fun NutritionDto.toDomain(): Nutrition {
        return Nutrition(
            energyKcal = this.energyKcal,
            proteinG = this.proteinG,
            fatG = this.fatG,
            carbohydrateG = this.carbohydrateG,
            sugarG = this.sugarG,
            saturatedFatG = this.saturatedFatG,
            cholesterolMg = this.cholesterolMg,
            fiberG = this.fiberG,
            sodiumMg = this.sodiumMg,
            calciumMg = this.calciumMg,
            ironMg = this.ironMg
        )
    }

    private fun NutritionTotalsDto.toDomain(): Nutrition {
        return Nutrition(
            energyKcal = this.energyKcal,
            proteinG = this.proteinG,
            fatG = this.fatG,
            carbohydrateG = this.carbohydrateG,
            sugarG = this.sugarG,
            saturatedFatG = this.saturatedFatG,
            cholesterolMg = this.cholesterolMg,
            fiberG = this.fiberG,
            sodiumMg = this.sodiumMg,
            calciumMg = this.calciumMg,
            ironMg = this.ironMg
        )
    }

    private fun NutritionSourceDto.toDomain(): NutritionSource {
        return NutritionSource(
            sourceName = this.sourceName,
            sourceVersion = this.sourceVersion
        )
    }
}