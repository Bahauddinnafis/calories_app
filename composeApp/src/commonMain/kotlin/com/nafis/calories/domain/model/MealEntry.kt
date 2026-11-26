package com.nafis.calories.domain.model

enum class MealSource(val key: String) {
    SCAN("scan"),
    RECOMMENDATION("recommendation"),
    PRODUCT("product"),
    MANUAL("manual");

    companion object {
        fun fromKey(key: String?): MealSource? = entries.firstOrNull { it.key == key }
    }
}

/** A logged meal as shown in lists; nutrition values are already scaled to the eaten portion. */
data class MealEntry(
    val id: Long,
    val scanId: String,
    val mealName: String,
    val timestamp: Long,
    val mealType: MealType,
    val source: MealSource,
    val imagePath: String?,
    val consumedWeightG: Double,
    val totalEnergyKcal: Double?,
    val energyKcal: Double?,
    val proteinG: Double?,
    val carbohydrateG: Double?,
    val fatG: Double?
) {
    val hasImage: Boolean get() = imagePath != null
}

data class ConsumedTotals(val energyKcal: Double, val proteinG: Double)