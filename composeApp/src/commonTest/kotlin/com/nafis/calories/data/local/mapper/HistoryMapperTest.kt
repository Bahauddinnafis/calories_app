package com.nafis.calories.data.local.mapper

import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.domain.model.MealSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryMapperTest {

    @Test
    fun decodesIngredientsWrittenByGsonWithoutNullFields() {
        val gsonJson = """[{"foodName":"Nasi putih","estimatedWeightG":150.0,"matchStatus":"matched",""" +
            """"matchConfidence":0.92,"energyKcal":195.0,"proteinG":3.6}]"""

        val result = HistoryMapper.toDomain(entity(ingredientsJson = gsonJson))

        val ingredient = result.ingredients.single()
        assertEquals("Nasi putih", ingredient.foodName)
        assertNull(ingredient.foodCode)
        assertEquals(195.0, ingredient.nutrition?.energyKcal)
        assertNull(ingredient.source)
    }

    @Test
    fun entryScalesNutritionToTheEatenPortion() {
        val entry = HistoryMapper.toEntry(entity(consumedWeightG = 100.0))

        assertEquals(200.0, entry.energyKcal)
        assertEquals(100.0, entry.consumedWeightG)
        assertNull(entry.imagePath)
    }

    @Test
    fun olderRowsWithoutAPortionCountTheWholePhoto() {
        val entry = HistoryMapper.toEntry(entity(consumedWeightG = null))

        assertEquals(400.0, entry.energyKcal)
        assertEquals(200.0, entry.consumedWeightG)
    }

    @Test
    fun legacyRowsInferTheirSourceFromTheScanId() {
        val entry = HistoryMapper.toEntry(entity(scanId = "product_8991234567890", logSource = null))

        assertEquals(MealSource.PRODUCT, entry.source)
    }

    private fun entity(
        scanId: String = "scan-1",
        ingredientsJson: String = "[]",
        consumedWeightG: Double? = null,
        logSource: String? = ScanHistoryEntity.SOURCE_SCAN
    ) = ScanHistoryEntity(
        scanId = scanId,
        mealName = "Nasi goreng",
        servingWeightG = 200.0,
        servingConfidence = 0.8,
        ingredientsJson = ingredientsJson,
        totalEnergyKcal = 400.0,
        totalProteinG = 12.0,
        totalFatG = 15.0,
        totalCarbohydrateG = 55.0,
        totalFiberG = null,
        totalSodiumMg = null,
        totalCalciumMg = null,
        totalIronMg = null,
        isComplete = true,
        missingNutrientsJson = "[]",
        overallConfidence = 0.8,
        uncertaintyNotesJson = "[]",
        imagePath = "",
        timestamp = 1_764_000_000_000,
        consumedWeightG = consumedWeightG,
        logSource = logSource
    )
}