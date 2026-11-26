package com.nafis.calories.domain.model

data class MealRecommendation(
    val id: String,
    val title: String,
    val description: String?,
    val category: String?,
    val reason: String?,
    val servingWeightG: Double,
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double
)

data class MealRecommendations(
    val category: String?,
    val items: List<MealRecommendation>
)

data class RemainingBudget(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double
)
