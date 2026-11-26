package com.nafis.calories.data.remote.mapper

import com.nafis.calories.domain.model.Ingredient
import com.nafis.calories.domain.model.Nutrition
import com.nafis.calories.domain.model.NutritionSource
import com.nafis.calories.domain.model.ScanError
import com.nafis.calories.domain.model.ScanNotes
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.model.ScanStatus
import com.nafis.calories.domain.model.ServingNormalizer
import com.nafis.calories.domain.model.SuggestedAddOn
import com.nafis.calories.data.remote.dto.*

object ScanMapper {

    fun toDomain(dto: ScanResponseDto): ScanResult {
        val aiNotice = dto.aiNotice?.trim()?.takeIf { it.isNotEmpty() } ?: dto.nutritionProvenance?.aiNoticeText
        val uncertaintyNotes = dto.analysis.uncertaintyNotes.orEmpty()
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
            uncertaintyNotes = uncertaintyNotes,
            personalNutritionNote = ScanNotes.personalNote(dto.nutritionNote, uncertaintyNotes),
            aiDisclaimerText = ScanNotes.aiDisclaimer(aiNotice),
            status = ScanStatus.resolve(dto.status, ingredients, totals, totalsComplete),
            errors = dto.errors.orEmpty().map { it.toDomain() },
            suggestedAddOns = dto.suggestedAddOns.orEmpty().mapNotNull { it.toDomain() }
        )
    }

    private fun SuggestedAddOnDto.toDomain(): SuggestedAddOn? {
        val id = addOnId?.takeIf { it.isNotBlank() } ?: return null
        val label = name?.takeIf { it.isNotBlank() } ?: return null
        val weight = defaultWeightG?.takeIf { it > 0.0 } ?: return null
        return SuggestedAddOn(addOnId = id, name = label, defaultWeightG = weight, energyKcal = energyKcal)
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
        val resolvedName = this.name?.takeIf { it.isNotBlank() } ?: this.sourceName ?: "Estimasi AI"
        return NutritionSource(
            sourceName = resolvedName,
            sourceVersion = this.sourceVersion ?: ""
        )
    }
}