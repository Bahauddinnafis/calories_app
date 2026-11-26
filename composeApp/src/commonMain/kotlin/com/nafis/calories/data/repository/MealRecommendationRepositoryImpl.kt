package com.nafis.calories.data.repository

import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.dto.MealRecommendationDto
import com.nafis.calories.data.remote.dto.MealRecommendationsResponseDto
import com.nafis.calories.domain.model.MealRecommendation
import com.nafis.calories.domain.model.MealRecommendations
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.RemainingBudget
import com.nafis.calories.domain.model.WeightGoal
import com.nafis.calories.domain.repository.MealRecommendationRepository
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

class MealRecommendationRepositoryImpl(private val api: CaloriesApi) : MealRecommendationRepository {

    override suspend fun fetch(
        remaining: RemainingBudget,
        mealType: MealType,
        goal: WeightGoal,
        preferences: Set<String>,
        allergies: Set<String>
    ): Result<MealRecommendations> = try {
        val response = api.getMealRecommendations(
            remainingKcal = remaining.energyKcal.coerceAtLeast(0.0),
            remainingProteinG = remaining.proteinG.coerceAtLeast(0.0),
            remainingCarbsG = remaining.carbohydrateG.coerceAtLeast(0.0),
            remainingFatG = remaining.fatG.coerceAtLeast(0.0),
            mealType = mealType.key,
            goal = goal.key,
            preferences = preferences.toQueryValue(),
            allergies = allergies.toQueryValue()
        )
        if (response.status.isSuccess()) {
            val body = response.body<MealRecommendationsResponseDto>()
            Result.success(
                MealRecommendations(
                    category = body.category?.takeIf { it.isNotBlank() },
                    items = body.recommendations.orEmpty().mapNotNull { it.toDomain() }
                )
            )
        } else {
            Result.failure(Exception("Rekomendasi menu belum bisa dimuat."))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet kamu."))
    }

    private fun Set<String>.toQueryValue(): String? = takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

    /** Only loggable with an id, a name, and its calories. */
    private fun MealRecommendationDto.toDomain(): MealRecommendation? {
        val id = mealId?.takeIf { it.isNotBlank() } ?: return null
        val title = title?.takeIf { it.isNotBlank() } ?: return null
        val nutrition = nutrition ?: return null
        val kcal = nutrition.energyKcal ?: return null
        return MealRecommendation(
            id = id,
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            category = category?.takeIf { it.isNotBlank() },
            reason = reason?.takeIf { it.isNotBlank() },
            servingWeightG = servingWeightG?.takeIf { it > 0.0 } ?: 0.0,
            energyKcal = kcal,
            proteinG = nutrition.proteinG ?: 0.0,
            carbohydrateG = nutrition.carbohydrateG ?: 0.0,
            fatG = nutrition.fatG ?: 0.0
        )
    }
}