package com.nafis.calories.data.remote.dto

import com.nafis.calories.domain.model.ScanCorrection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecalculateRequestDto(
    @SerialName("meal_name")
    val mealName: String,

    @SerialName("ingredients")
    val ingredients: List<RecalculateIngredientDto>,

    @SerialName("add_ons")
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

@Serializable
data class RecalculateIngredientDto(
    @SerialName("food_name")
    val foodName: String,

    @SerialName("estimated_weight_g")
    val estimatedWeightG: Double
)

@Serializable
data class AddOnSelectionDto(
    @SerialName("add_on_id")
    val addOnId: String,

    @SerialName("weight_g")
    val weightG: Double
)