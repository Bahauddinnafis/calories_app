package com.nafis.calories.data.model

import kotlin.math.roundToInt

data class IngredientCorrection(
    val foodName: String,
    val weightG: Double
)

data class AddOnSelection(
    val addOnId: String,
    val weightG: Double
)

data class ScanCorrection(
    val mealName: String,
    val ingredients: List<IngredientCorrection>,
    val addOns: List<AddOnSelection> = emptyList()
) {
    val totalWeightG: Double get() = ingredients.sumOf { it.weightG }
}

object ScanCorrectionRules {
    const val MAX_NAME_LENGTH = 200
    const val MAX_INGREDIENTS = 25
    const val MAX_WEIGHT_G = 5000.0

    fun isValidName(name: String): Boolean = name.isNotBlank() && name.trim().length <= MAX_NAME_LENGTH

    fun isValidWeight(weightG: Double?): Boolean = weightG != null && weightG > 0.0 && weightG <= MAX_WEIGHT_G

    fun validate(correction: ScanCorrection): String? = when {
        !isValidName(correction.mealName) -> "Nama makanan belum diisi."
        correction.ingredients.isEmpty() -> "Tambahkan minimal satu bahan."
        correction.ingredients.size > MAX_INGREDIENTS -> "Maksimal $MAX_INGREDIENTS bahan."
        correction.ingredients.any { !isValidName(it.foodName) } -> "Nama setiap bahan harus diisi."
        correction.ingredients.any { !isValidWeight(it.weightG) } ->
            "Berat setiap bahan harus lebih dari 0 dan maksimal ${MAX_WEIGHT_G.roundToInt()} g."
        else -> null
    }

    fun prefill(result: ScanResult): ScanCorrection {
        val consumed = result.consumedWeightG ?: result.defaultConsumedWeightG
        val factor = if (result.servingWeightG > 0) consumed / result.servingWeightG else 1.0
        return ScanCorrection(
            mealName = result.mealName,
            ingredients = result.ingredients.map {
                IngredientCorrection(
                    foodName = it.foodName,
                    weightG = (it.estimatedWeightG * factor * 10).roundToInt() / 10.0
                )
            }
        )
    }
}