package com.nafis.calories.domain.model

data class PackagedProduct(
    val id: String,
    val name: String,
    val brand: String?,
    val servingLabel: String?,
    val servingAmount: Double?,
    val servingUnit: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val servingWeightG: Double
)
