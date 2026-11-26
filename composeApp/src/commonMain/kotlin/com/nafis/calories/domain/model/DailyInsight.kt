package com.nafis.calories.domain.model

data class DailyInsight(
    val headline: String?,
    val message: String?,
    val status: String?,
    val priorityFocus: String?,
    val hydrationNudge: String?,
    val suggestedCategory: String?,
    val remainingKcal: Double?,
    val remainingProteinG: Double?
)

data class DailyInsightQuery(
    val consumedKcal: Double,
    val targetKcal: Double,
    val proteinG: Double,
    val targetProteinG: Double,
    val carbohydrateG: Double,
    val targetCarbohydrateG: Double,
    val fatG: Double,
    val targetFatG: Double,
    val waterGlasses: Int,
    val targetGlasses: Int,
    val mealCount: Int,
    val goal: WeightGoal
)
