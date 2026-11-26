package com.nafis.calories.domain.repository

import com.nafis.calories.domain.model.DailyInsight
import com.nafis.calories.domain.model.DailyInsightQuery
import com.nafis.calories.domain.model.MealRecommendations
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.PackagedProduct
import com.nafis.calories.domain.model.RemainingBudget
import com.nafis.calories.domain.model.WeightGoal

interface PackagedProductRepository {
    fun isBarcode(input: String): Boolean
    suspend fun search(input: String): Result<List<PackagedProduct>>
}

interface MealRecommendationRepository {
    suspend fun fetch(
        remaining: RemainingBudget,
        mealType: MealType,
        goal: WeightGoal,
        preferences: Set<String>,
        allergies: Set<String>
    ): Result<MealRecommendations>
}

interface InsightRepository {
    suspend fun fetchDailyInsight(query: DailyInsightQuery): Result<DailyInsight>
}
