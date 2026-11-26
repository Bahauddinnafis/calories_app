package com.nafis.calories.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanResultTest {

    private fun result(
        total: Double = 6.0,
        single: Double? = 2.0,
        count: Int? = 3,
        serving: Double = 0.8
    ) = ScanResult(
        scanId = "id",
        mealName = "Kopi sachet",
        servingWeightG = total,
        singleServingWeightG = single,
        servingUnitName = "sachet",
        servingUnitCount = count,
        servingConfidence = serving,
        ingredients = emptyList(),
        totalNutrition = Nutrition.empty(),
        isComplete = true,
        missingNutrients = emptyList(),
        overallConfidence = 0.8,
        uncertaintyNotes = emptyList()
    )

    @Test
    fun confidenceBuckets() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.from(0.75))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceLevel.from(0.5))
        assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.from(0.49))
    }

    @Test
    fun missingConfidenceIsNotReportedAsLow() {
        assertNull(ConfidenceLevel.from(0.0))
        assertNull(ConfidenceLevel.from(Double.NaN))
    }

    @Test
    fun servingIsUncertainOnlyWhenItsOwnConfidenceIsLow() {
        assertTrue(result(serving = 0.3).isServingUncertain)
        assertFalse(result(serving = 0.6).isServingUncertain)
    }

    @Test
    fun defaultConsumedPortionIsOneUnitForMultiUnitResults() {
        assertEquals(2.0, result().defaultConsumedWeightG)
    }

    @Test
    fun defaultConsumedPortionIsTheWholePhotoForSinglePortions() {
        assertEquals(250.0, result(total = 250.0, single = 250.0, count = 1).defaultConsumedWeightG)
    }

    @Test
    fun newResultsAreTimestampedNow() {
        assertTrue(result().timestamp > 0L)
    }

    @Test
    fun mealTypeIsSuggestedFromTheHour() {
        assertEquals(MealType.BREAKFAST, MealType.fromHour(7))
        assertEquals(MealType.LUNCH, MealType.fromHour(12))
        assertEquals(MealType.DINNER, MealType.fromHour(19))
        assertEquals(MealType.SNACK, MealType.fromHour(15))
        assertEquals(MealType.SNACK, MealType.fromHour(23))
    }

    @Test
    fun unknownMealTypeKeyIsNull() {
        assertNull(MealType.fromKey("brunch"))
        assertEquals(MealType.DINNER, MealType.fromKey("dinner"))
    }
}
