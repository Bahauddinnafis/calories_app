package com.nafis.calories.data.model

import com.nafis.calories.data.local.entity.ScanHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultUserFieldsTest {

    private fun result(
        total: Double = 6.0,
        single: Double? = 2.0,
        count: Int? = 3,
        overall: Double = 0.8,
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
        overallConfidence = overall,
        uncertaintyNotes = emptyList()
    )

    @Test
    fun `confidence buckets`() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.from(0.75))
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceLevel.from(0.5))
        assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.from(0.49))
    }

    @Test
    fun `missing confidence is not reported as low`() {
        // Gson leaves absent doubles at 0.0, which must not show a "rough estimate" banner.
        assertNull(ConfidenceLevel.from(0.0))
        assertNull(ConfidenceLevel.from(Double.NaN))
    }

    @Test
    fun `serving is uncertain only when its own confidence is low`() {
        assertTrue(result(serving = 0.3).isServingUncertain)
        assertFalse(result(serving = 0.6).isServingUncertain)
    }

    @Test
    fun `default consumed portion is one unit for multi-unit results`() {
        assertEquals(2.0, result().defaultConsumedWeightG, 0.0)
    }

    @Test
    fun `default consumed portion is the whole photo for single portions`() {
        assertEquals(250.0, result(total = 250.0, single = 250.0, count = 1).defaultConsumedWeightG, 0.0)
    }

    @Test
    fun `meal type suggested from hour`() {
        assertEquals(MealType.BREAKFAST, MealType.fromHour(7))
        assertEquals(MealType.LUNCH, MealType.fromHour(12))
        assertEquals(MealType.DINNER, MealType.fromHour(19))
        assertEquals(MealType.SNACK, MealType.fromHour(15))
        assertEquals(MealType.SNACK, MealType.fromHour(23))
    }

    @Test
    fun `unknown meal type key is null`() {
        assertNull(MealType.fromKey("brunch"))
        assertEquals(MealType.DINNER, MealType.fromKey("dinner"))
    }

    private fun entity(servingWeightG: Double, consumedWeightG: Double?) = ScanHistoryEntity(
        scanId = "id",
        mealName = "Kopi sachet",
        servingWeightG = servingWeightG,
        servingConfidence = 0.8,
        ingredientsJson = "[]",
        totalEnergyKcal = 21.0,
        totalProteinG = null,
        totalFatG = null,
        totalCarbohydrateG = null,
        totalFiberG = null,
        totalSodiumMg = null,
        totalCalciumMg = null,
        totalIronMg = null,
        isComplete = true,
        missingNutrientsJson = "[]",
        overallConfidence = 0.8,
        uncertaintyNotesJson = "[]",
        imagePath = "",
        consumedWeightG = consumedWeightG
    )

    @Test
    fun `history counts only the consumed portion`() {
        assertEquals(7.0, entity(6.0, 2.0).consumedEnergyKcal!!, 0.001)
    }

    @Test
    fun `older history rows without a portion count the whole photo`() {
        assertEquals(21.0, entity(6.0, null).consumedEnergyKcal!!, 0.0)
    }
}
