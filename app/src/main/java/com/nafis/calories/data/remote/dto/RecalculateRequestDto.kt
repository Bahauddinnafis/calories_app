package com.nafis.calories.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nafis.calories.data.model.ScanCorrection

/** Body of `POST /api/v1/scans/{scan_id}/recalculate` and `POST /api/v1/nutrition/estimate`. */
data class RecalculateRequestDto(
    @SerializedName("meal_name")
    val mealName: String,

    @SerializedName("ingredients")
    val ingredients: List<RecalculateIngredientDto>,

    @SerializedName("add_ons")
    val addOns: List<AddOnSelectionDto> = emptyList()
) {
    companion object {
        fun from(correction: ScanCorrection) = RecalculateRequestDto(
            mealName = correction.mealName.trim(),
            ingredients = correction.ingredients.map {
                RecalculateIngredientDto(foodName = it.foodName.trim(), estimatedWeightG = it.weightG)
            },
            addOns = correction.addOns.map { AddOnSelectionDto(addOnId = it.addOnId, weightG = it.weightG) }
        )
    }
}

data class RecalculateIngredientDto(
    @SerializedName("food_name")
    val foodName: String,

    @SerializedName("estimated_weight_g")
    val estimatedWeightG: Double
)

data class AddOnSelectionDto(
    @SerializedName("add_on_id")
    val addOnId: String,

    @SerializedName("weight_g")
    val weightG: Double
)
