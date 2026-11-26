package com.nafis.calories.data.remote.mapper

import com.nafis.calories.data.remote.dto.AnalysisDto
import com.nafis.calories.data.remote.dto.IngredientDto
import com.nafis.calories.data.remote.dto.NutritionDto
import com.nafis.calories.data.remote.dto.NutritionSourceDto
import com.nafis.calories.data.remote.dto.NutritionTotalsDto
import com.nafis.calories.data.remote.dto.ScanErrorDto
import com.nafis.calories.data.remote.dto.ScanResponseDto
import com.nafis.calories.data.remote.dto.ServingEstimateDto
import com.nafis.calories.domain.model.ScanStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanMapperTest {

    @Test
    fun convertsACompleteResponse() {
        val result = ScanMapper.toDomain(coffeeSachetDto())

        assertEquals("scan-coffee", result.scanId)
        assertEquals("Nescafe Ice Roast Americano", result.mealName)
        assertEquals(6.0, result.servingWeightG)
        assertEquals(0.95, result.servingConfidence)
        assertEquals("instant coffee powder", result.ingredients.single().foodName)
        assertEquals(21.18, result.totalNutrition.energyKcal)
        assertTrue(result.isComplete)
        assertEquals(ScanStatus.COMPLETED, result.status)
    }

    @Test
    fun keepsNullNutritionAsNull() {
        val result = ScanMapper.toDomain(coffeeSachetDto(nutrition = null))

        assertNull(result.totalNutrition.energyKcal)
        assertNull(result.totalNutrition.proteinG)
        assertFalse(result.isComplete)
        assertNull(result.ingredients.single().nutrition)
        assertNull(result.ingredients.single().source)
    }

    @Test
    fun legacyCompletedStatusWithoutNutritionResolvesToNutritionUnavailable() {
        val result = ScanMapper.toDomain(coffeeSachetDto(status = "completed", nutrition = null))

        assertEquals(ScanStatus.NUTRITION_UNAVAILABLE, result.status)
        assertFalse(result.ingredients.single().hasNutrition)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.isRetryable)
    }

    @Test
    fun retryableErrorIsMapped() {
        val dto = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            errors = listOf(ScanErrorDto(stage = "ai_estimation", code = "provider_timeout", retryable = true))
        )

        val result = ScanMapper.toDomain(dto)

        assertEquals(ScanStatus.NUTRITION_UNAVAILABLE, result.status)
        assertEquals("provider_timeout", result.errors.single().code)
        assertTrue(result.isRetryable)
    }

    @Test
    fun nonRetryableOrUnflaggedErrorsDisableRetry() {
        val explicit = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            errors = listOf(ScanErrorDto(stage = "ai_estimation", code = "estimation_failed", retryable = false))
        )
        val missingFlag = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            errors = listOf(ScanErrorDto(stage = "ai_estimation", code = "estimation_failed"))
        )

        assertFalse(ScanMapper.toDomain(explicit).isRetryable)
        assertFalse(ScanMapper.toDomain(missingFlag).isRetryable)
    }

    @Test
    fun incompleteTotalsResolveToPartialEvenIfBackendSaysCompleted() {
        assertEquals(ScanStatus.PARTIAL, ScanMapper.toDomain(coffeeSachetDto(complete = false)).status)
    }

    @Test
    fun backendPartialStatusIsKept() {
        assertEquals(ScanStatus.PARTIAL, ScanMapper.toDomain(coffeeSachetDto(status = "partial")).status)
    }

    @Test
    fun unknownStatusFallsBackToTheStatusDerivedFromData() {
        assertEquals(ScanStatus.COMPLETED, ScanMapper.toDomain(coffeeSachetDto(status = "some_future_status")).status)
    }

    @Test
    fun validMultiUnitServingIsKept() {
        val result = ScanMapper.toDomain(coffeeSachetDto())

        assertEquals(2.0, result.singleServingWeightG)
        assertEquals("sachet", result.servingUnitName)
        assertEquals(3, result.servingUnitCount)
        assertTrue(result.isMultiUnit)
    }

    @Test
    fun singleServingHeavierThanTotalFallsBackToOnePortion() {
        val result = ScanMapper.toDomain(coffeeSachetDto(single = 25.0))

        assertEquals(6.0, result.singleServingWeightG)
        assertEquals(1, result.servingUnitCount)
        assertEquals("porsi", result.servingUnitName)
        assertFalse(result.isMultiUnit)
    }

    @Test
    fun singleTimesCountNotMatchingTotalFallsBackToOnePortion() {
        val result = ScanMapper.toDomain(coffeeSachetDto(total = 240.0, single = 100.0, unitName = "potong", unitCount = 2))

        assertEquals(240.0, result.singleServingWeightG)
        assertEquals(1, result.servingUnitCount)
    }

    @Test
    fun roundedSingleServingWithinToleranceIsAccepted() {
        val result = ScanMapper.toDomain(coffeeSachetDto(total = 100.0, single = 33.3, unitName = "butir", unitCount = 3))

        assertEquals(3, result.servingUnitCount)
        assertEquals(33.3, result.singleServingWeightG)
    }

    @Test
    fun responseWithoutServingFieldsDefaultsToOnePortionOfTotal() {
        val result = ScanMapper.toDomain(coffeeSachetDto(single = null, unitName = null, unitCount = null))

        assertEquals(6.0, result.singleServingWeightG)
        assertEquals(1, result.servingUnitCount)
        assertEquals("porsi", result.servingUnitName)
    }

    @Test
    fun zeroUnitCountFallsBackToOnePortion() {
        assertEquals(1, ScanMapper.toDomain(coffeeSachetDto(unitCount = 0)).servingUnitCount)
    }

    private fun coffeeSachetDto(
        status: String = "completed",
        total: Double = 6.0,
        single: Double? = 2.0,
        unitName: String? = "sachet",
        unitCount: Int? = 3,
        nutrition: NutritionDto? = NutritionDto(
            energyKcal = 21.18,
            proteinG = 0.73,
            fatG = 0.03,
            carbohydrateG = 4.52,
            fiberG = 0.0,
            sodiumMg = 2.22,
            calciumMg = 8.46,
            ironMg = 0.26
        ),
        complete: Boolean = nutrition != null,
        errors: List<ScanErrorDto>? = null
    ) = ScanResponseDto(
        scanId = "scan-coffee",
        status = status,
        mealName = "Nescafe Ice Roast Americano",
        servingEstimate = ServingEstimateDto(
            totalWeightG = total,
            singleServingWeightG = single,
            servingUnitName = unitName,
            servingUnitCount = unitCount,
            confidence = 0.95
        ),
        ingredients = listOf(
            IngredientDto(
                foodCode = null,
                foodName = "instant coffee powder",
                estimatedWeightG = total,
                matchStatus = if (nutrition != null) "estimated" else "unresolved",
                matchConfidence = 0.83,
                nutritionSourceType = if (nutrition != null) "ai_estimation" else null,
                nutritionStatus = if (nutrition != null) "ai_estimated" else "unavailable",
                nutrition = nutrition,
                source = nutrition?.let { NutritionSourceDto(sourceName = "AI nutrition estimate", sourceVersion = "test") }
            )
        ),
        totals = NutritionTotalsDto(
            energyKcal = nutrition?.energyKcal,
            proteinG = nutrition?.proteinG,
            fatG = nutrition?.fatG,
            carbohydrateG = nutrition?.carbohydrateG,
            fiberG = nutrition?.fiberG,
            sodiumMg = nutrition?.sodiumMg,
            calciumMg = nutrition?.calciumMg,
            ironMg = nutrition?.ironMg,
            complete = complete,
            missingNutrients = if (complete) emptyList() else listOf("energy_kcal")
        ),
        analysis = AnalysisDto(overallConfidence = 0.95, uncertaintyNotes = emptyList()),
        errors = errors,
        createdAt = "2026-11-23T03:57:17Z"
    )
}
