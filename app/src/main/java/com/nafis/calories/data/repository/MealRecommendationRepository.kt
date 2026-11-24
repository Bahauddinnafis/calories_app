package com.nafis.calories.data.repository

import android.util.Log
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.model.WeightGoal
import com.nafis.calories.data.remote.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One serving of a recommended meal; nutrition values are per serving, as logged. */
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

/** What is still left of today's targets; the backend filters menus by these. */
data class RemainingBudget(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double
)

/** Fetches meal recommendations from `GET /api/v1/recommendations/meals`. */
object MealRecommendationRepository {

    private const val TAG = "MealRecommendations"

    suspend fun fetch(
        remaining: RemainingBudget,
        mealType: MealType,
        goal: WeightGoal,
        preferences: Set<String>,
        allergies: Set<String>
    ): Result<MealRecommendations> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.getMealRecommendations(
                remainingKcal = remaining.energyKcal.coerceAtLeast(0.0),
                remainingProteinG = remaining.proteinG.coerceAtLeast(0.0),
                remainingCarbsG = remaining.carbohydrateG.coerceAtLeast(0.0),
                remainingFatG = remaining.fatG.coerceAtLeast(0.0),
                mealType = mealType.key,
                goal = goal.key,
                preferences = preferences.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
                allergies = allergies.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                Log.w(TAG, "Recommendations failed: HTTP ${response.code()}")
                return@withContext Result.failure(Exception("Rekomendasi menu belum bisa dimuat."))
            }
            val items = body.recommendations.orEmpty().mapNotNull { dto ->
                // A recommendation is only loggable with an id, a name, and its calories.
                val id = dto.mealId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = dto.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val kcal = dto.nutrition?.energyKcal ?: return@mapNotNull null
                MealRecommendation(
                    id = id,
                    title = title,
                    description = dto.description?.takeIf { it.isNotBlank() },
                    category = dto.category?.takeIf { it.isNotBlank() },
                    reason = dto.reason?.takeIf { it.isNotBlank() },
                    servingWeightG = dto.servingWeightG?.takeIf { it > 0.0 } ?: 0.0,
                    energyKcal = kcal,
                    proteinG = dto.nutrition.proteinG ?: 0.0,
                    carbohydrateG = dto.nutrition.carbohydrateG ?: 0.0,
                    fatG = dto.nutrition.fatG ?: 0.0
                )
            }
            Result.success(MealRecommendations(category = body.category?.takeIf { it.isNotBlank() }, items = items))
        } catch (e: Exception) {
            Log.w(TAG, "Recommendations request error", e)
            Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet kamu."))
        }
    }
}
