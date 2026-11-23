package com.nafis.calories.data.remote.mapper

import com.nafis.calories.data.model.ScanStatus
import com.nafis.calories.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScanMapper.
 * Tests DTO to domain model conversion.
 */
class ScanMapperTest {

    @Test
    fun `toDomain converts complete ScanResponseDto correctly`() {
        // Given: Complete backend response
        val dto = ScanResponseDto(
            scanId = "test-scan-123",
            status = "completed",
            mealName = "Nasi Goreng",
            servingEstimate = ServingEstimateDto(
                totalWeightG = 350.0,
                confidence = 0.82
            ),
            ingredients = listOf(
                IngredientDto(
                    foodCode = "rice-001",
                    foodName = "Nasi putih",
                    estimatedWeightG = 250.0,
                    matchStatus = "matched",
                    matchConfidence = 0.91,
                    nutrition = NutritionDto(
                        energyKcal = 450.0,
                        proteinG = 12.0,
                        fatG = 15.0,
                        carbohydrateG = 60.0,
                        sugarG = 2.5,
                        saturatedFatG = 3.0,
                        cholesterolMg = 0.0,
                        fiberG = 2.0,
                        sodiumMg = 500.0,
                        calciumMg = 20.0,
                        ironMg = 1.5
                    ),
                    source = NutritionSourceDto(
                        sourceName = "TKPI",
                        sourceVersion = "2020"
                    )
                )
            ),
            totals = NutritionTotalsDto(
                energyKcal = 520.0,
                proteinG = 18.0,
                fatG = 20.0,
                carbohydrateG = 65.0,
                sugarG = 14.5,
                saturatedFatG = 4.2,
                cholesterolMg = 45.0,
                fiberG = 3.0,
                sodiumMg = 650.0,
                calciumMg = 35.0,
                ironMg = 2.0,
                complete = true,
                missingNutrients = emptyList()
            ),
            analysis = AnalysisDto(
                overallConfidence = 0.84,
                uncertaintyNotes = listOf("Ingredient quantities are estimated from the image.")
            ),
            createdAt = "2026-11-18T09:00:00Z"
        )

        // When: Convert to domain model
        val scanResult = ScanMapper.toDomain(dto)

        // Then: Verify conversion
        assertEquals("test-scan-123", scanResult.scanId)
        assertEquals("Nasi Goreng", scanResult.mealName)
        assertEquals(350.0, scanResult.servingWeightG, 0.01)
        assertEquals(0.82, scanResult.servingConfidence, 0.01)
        assertEquals(1, scanResult.ingredients.size)
        assertEquals("Nasi putih", scanResult.ingredients[0].foodName)
        assertEquals(520.0, scanResult.totalNutrition.energyKcal!!, 0.01)
        assertEquals(14.5, scanResult.totalNutrition.sugarG!!, 0.01)
        assertEquals(4.2, scanResult.totalNutrition.saturatedFatG!!, 0.01)
        assertEquals(45.0, scanResult.totalNutrition.cholesterolMg!!, 0.01)
        assertTrue(scanResult.isComplete)
        assertEquals(0.84, scanResult.overallConfidence, 0.01)
    }

    @Test
    fun `toDomain handles null nutrition values correctly`() {
        // Given: Response with null nutrition values
        val dto = ScanResponseDto(
            scanId = "test-scan-456",
            status = "completed",
            mealName = "Unknown Food",
            servingEstimate = ServingEstimateDto(
                totalWeightG = 100.0,
                confidence = 0.5
            ),
            ingredients = listOf(
                IngredientDto(
                    foodCode = null,
                    foodName = "Unknown ingredient",
                    estimatedWeightG = 100.0,
                    matchStatus = "unresolved",
                    matchConfidence = 0.3,
                    nutrition = null,
                    source = null
                )
            ),
            totals = NutritionTotalsDto(
                energyKcal = null,
                proteinG = null,
                fatG = null,
                carbohydrateG = null,
                fiberG = null,
                sodiumMg = null,
                calciumMg = null,
                ironMg = null,
                complete = false,
                missingNutrients = listOf("energy", "protein", "fat", "carbohydrate")
            ),
            analysis = AnalysisDto(
                overallConfidence = 0.5,
                uncertaintyNotes = listOf("Low confidence due to unclear image.")
            ),
            createdAt = "2026-11-18T09:00:00Z"
        )

        // When: Convert to domain model
        val scanResult = ScanMapper.toDomain(dto)

        // Then: Verify null handling
        assertNull(scanResult.totalNutrition.energyKcal)
        assertNull(scanResult.totalNutrition.proteinG)
        assertFalse(scanResult.isComplete)
        assertEquals(4, scanResult.missingNutrients.size)
        assertNull(scanResult.ingredients[0].nutrition)
        assertNull(scanResult.ingredients[0].source)
        assertEquals(ScanStatus.NUTRITION_UNAVAILABLE, scanResult.status)
    }

    @Test
    fun `toDomain handles empty ingredients list`() {
        // Given: Response with no ingredients
        val dto = ScanResponseDto(
            scanId = "test-scan-789",
            status = "completed",
            mealName = "Simple Food",
            servingEstimate = ServingEstimateDto(
                totalWeightG = 50.0,
                confidence = 0.9
            ),
            ingredients = emptyList(),
            totals = NutritionTotalsDto(
                energyKcal = 100.0,
                proteinG = 5.0,
                fatG = 2.0,
                carbohydrateG = 15.0,
                fiberG = 1.0,
                sodiumMg = 50.0,
                calciumMg = 10.0,
                ironMg = 0.5,
                complete = true,
                missingNutrients = emptyList()
            ),
            analysis = AnalysisDto(
                overallConfidence = 0.9,
                uncertaintyNotes = emptyList()
            ),
            createdAt = "2026-11-18T09:00:00Z"
        )

        // When: Convert to domain model
        val scanResult = ScanMapper.toDomain(dto)

        // Then: Verify empty list handling
        assertTrue(scanResult.ingredients.isEmpty())
        assertTrue(scanResult.isComplete)
        assertTrue(scanResult.uncertaintyNotes.isEmpty())
        assertEquals(ScanStatus.COMPLETED, scanResult.status)
    }

    // region Status & errors contract

    @Test
    fun `legacy completed status without any nutrition resolves to nutrition_unavailable`() {
        // Mirrors scan 4e68c7b3: backend said "completed" but every value was null.
        val dto = coffeeSachetDto(status = "completed", nutrition = null, nutritionStatus = "unavailable")

        val result = ScanMapper.toDomain(dto)

        assertEquals(ScanStatus.NUTRITION_UNAVAILABLE, result.status)
        assertFalse(result.ingredients[0].hasNutrition)
        // No errors field (older backend) -> retry must still be offered.
        assertTrue(result.errors.isEmpty())
        assertTrue(result.isRetryable)
    }

    @Test
    fun `nutrition_unavailable with retryable error is mapped`() {
        val dto = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            nutritionStatus = "unavailable",
            errors = listOf(
                ScanErrorDto(
                    stage = "ai_estimation",
                    code = "provider_timeout",
                    message = "AI nutrition estimation was unavailable",
                    retryable = true
                )
            )
        )

        val result = ScanMapper.toDomain(dto)

        assertEquals(ScanStatus.NUTRITION_UNAVAILABLE, result.status)
        assertEquals(1, result.errors.size)
        assertEquals("provider_timeout", result.errors[0].code)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `non retryable errors disable retry`() {
        val dto = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            nutritionStatus = "unavailable",
            errors = listOf(ScanErrorDto(stage = "ai_estimation", code = "estimation_failed", retryable = false))
        )

        assertFalse(ScanMapper.toDomain(dto).isRetryable)
    }

    @Test
    fun `missing retryable flag is treated as not retryable`() {
        val dto = coffeeSachetDto(
            status = "nutrition_unavailable",
            nutrition = null,
            errors = listOf(ScanErrorDto(stage = "ai_estimation", code = "estimation_failed"))
        )

        assertFalse(ScanMapper.toDomain(dto).isRetryable)
    }

    @Test
    fun `incomplete totals with nutrition resolve to partial even if backend says completed`() {
        val dto = coffeeSachetDto(status = "completed", complete = false)

        assertEquals(ScanStatus.PARTIAL, ScanMapper.toDomain(dto).status)
    }

    @Test
    fun `backend partial status is kept when data looks complete`() {
        val dto = coffeeSachetDto(status = "partial")

        assertEquals(ScanStatus.PARTIAL, ScanMapper.toDomain(dto).status)
    }

    @Test
    fun `unknown status falls back to the status derived from data`() {
        val dto = coffeeSachetDto(status = "some_future_status")

        assertEquals(ScanStatus.COMPLETED, ScanMapper.toDomain(dto).status)
    }

    // endregion

    // region Serving estimate

    @Test
    fun `valid multi unit serving is kept`() {
        val dto = coffeeSachetDto(total = 6.0, single = 2.0, unitName = "sachet", unitCount = 3)

        val result = ScanMapper.toDomain(dto)

        assertEquals(6.0, result.servingWeightG, 0.001)
        assertEquals(2.0, result.singleServingWeightG!!, 0.001)
        assertEquals("sachet", result.servingUnitName)
        assertEquals(3, result.servingUnitCount)
        assertTrue(result.isMultiUnit)
    }

    @Test
    fun `single serving heavier than total falls back to one portion`() {
        // The 25g-vs-6g mismatch that produced a ~4x calorie error on screen.
        val dto = coffeeSachetDto(total = 6.0, single = 25.0, unitName = "sachet", unitCount = 3)

        val result = ScanMapper.toDomain(dto)

        assertEquals(6.0, result.singleServingWeightG!!, 0.001)
        assertEquals(1, result.servingUnitCount)
        assertEquals("porsi", result.servingUnitName)
        assertFalse(result.isMultiUnit)
    }

    @Test
    fun `single times count not matching total falls back to one portion`() {
        val dto = coffeeSachetDto(total = 240.0, single = 100.0, unitName = "potong", unitCount = 2)

        val result = ScanMapper.toDomain(dto)

        assertEquals(240.0, result.singleServingWeightG!!, 0.001)
        assertEquals(1, result.servingUnitCount)
    }

    @Test
    fun `rounded single serving within tolerance is accepted`() {
        // 100 / 3 = 33.33 -> backend rounds to 33.3
        val dto = coffeeSachetDto(total = 100.0, single = 33.3, unitName = "butir", unitCount = 3)

        val result = ScanMapper.toDomain(dto)

        assertEquals(3, result.servingUnitCount)
        assertEquals(33.3, result.singleServingWeightG!!, 0.001)
    }

    @Test
    fun `legacy response without serving fields defaults to one portion of total`() {
        val dto = coffeeSachetDto(total = 6.0, single = null, unitName = null, unitCount = null)

        val result = ScanMapper.toDomain(dto)

        assertEquals(6.0, result.singleServingWeightG!!, 0.001)
        assertEquals(1, result.servingUnitCount)
        assertEquals("porsi", result.servingUnitName)
    }

    @Test
    fun `zero or negative unit count falls back to one portion`() {
        val dto = coffeeSachetDto(total = 6.0, single = 2.0, unitName = "sachet", unitCount = 0)

        assertEquals(1, ScanMapper.toDomain(dto).servingUnitCount)
    }

    // endregion

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
        nutritionStatus: String? = if (nutrition != null) "ai_estimated" else "unavailable",
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
                nutritionStatus = nutritionStatus,
                nutrition = nutrition,
                source = nutrition?.let { NutritionSourceDto("AI nutrition estimate", "test") }
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
