package com.nafis.calories.data.model

import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScanCorrectionRulesTest {

    private fun correction(vararg items: Pair<String, Double>, meal: String = "Nasi Kuning") =
        ScanCorrection(meal, items.map { IngredientCorrection(it.first, it.second) })

    @Test
    fun `valid correction passes`() {
        assertNull(ScanCorrectionRules.validate(correction("nasi kuning" to 150.0, "telur dadar" to 50.0)))
    }

    @Test
    fun `blank meal name is rejected`() {
        assertNotNull(ScanCorrectionRules.validate(correction("nasi" to 100.0, meal = "  ")))
    }

    @Test
    fun `empty ingredient list is rejected`() {
        assertNotNull(ScanCorrectionRules.validate(correction()))
    }

    @Test
    fun `more than 25 ingredients is rejected`() {
        val items = Array(26) { "bahan $it" to 10.0 }
        assertNotNull(ScanCorrectionRules.validate(correction(*items)))
    }

    @Test
    fun `blank ingredient name is rejected`() {
        assertNotNull(ScanCorrectionRules.validate(correction("" to 100.0)))
    }

    @Test
    fun `weight must be above zero and at most 5000 g`() {
        assertNotNull(ScanCorrectionRules.validate(correction("nasi" to 0.0)))
        assertNotNull(ScanCorrectionRules.validate(correction("nasi" to 5000.1)))
        assertNull(ScanCorrectionRules.validate(correction("nasi" to 5000.0)))
    }

    @Test
    fun `prefill scales weights to the portion eaten`() {
        // Photo shows 3 sachets (6 g); the user ate 1 (2 g), so each ingredient is scaled by 1/3.
        val result = ScanResult(
            scanId = "id",
            mealName = "Kopi",
            servingWeightG = 6.0,
            singleServingWeightG = 2.0,
            servingUnitName = "sachet",
            servingUnitCount = 3,
            servingConfidence = 0.8,
            ingredients = listOf(
                Ingredient(null, "kopi bubuk", 6.0, "matched", 0.9, nutrition = null, source = null)
            ),
            totalNutrition = Nutrition.empty(),
            isComplete = true,
            missingNutrients = emptyList(),
            overallConfidence = 0.8,
            uncertaintyNotes = emptyList(),
            consumedWeightG = 2.0
        )
        val prefill = ScanCorrectionRules.prefill(result)
        assertEquals("Kopi", prefill.mealName)
        assertEquals(2.0, prefill.ingredients.single().weightG, 0.0)
    }

    @Test
    fun `request trims names and keeps weights`() {
        val dto = RecalculateRequestDto.from(correction(" telur ayam " to 50.0, meal = " Nasi Goreng "))
        assertEquals("Nasi Goreng", dto.mealName)
        assertEquals("telur ayam", dto.ingredients.single().foodName)
        assertEquals(50.0, dto.ingredients.single().estimatedWeightG, 0.0)
    }
}
