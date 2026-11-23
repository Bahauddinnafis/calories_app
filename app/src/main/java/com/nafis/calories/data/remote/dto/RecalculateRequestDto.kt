package com.nafis.calories.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nafis.calories.data.model.ScanCorrection

/** Body of `POST /api/v1/scans/{scan_id}/recalculate`. */
data class RecalculateRequestDto(
    @SerializedName("meal_name")
    val mealName: String,

    @SerializedName("ingredients")
    val ingredients: List<RecalculateIngredientDto>
) {
    companion object {
        fun from(correction: ScanCorrection) = RecalculateRequestDto(
            mealName = correction.mealName.trim(),
            ingredients = correction.ingredients.map {
                RecalculateIngredientDto(foodName = it.foodName.trim(), estimatedWeightG = it.weightG)
            }
        )
    }
}

data class RecalculateIngredientDto(
    @SerializedName("food_name")
    val foodName: String,

    @SerializedName("estimated_weight_g")
    val estimatedWeightG: Double
)