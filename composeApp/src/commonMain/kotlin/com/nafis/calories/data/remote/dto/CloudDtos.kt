package com.nafis.calories.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorEnvelopeDto(
    @SerialName("error") val error: ApiErrorDto? = null
)

@Serializable
data class ApiErrorDto(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("retryable") val retryable: Boolean? = null
)

@Serializable
data class BasicNutritionDto(
    @SerialName("energy_kcal") val energyKcal: Double? = null,
    @SerialName("protein_g") val proteinG: Double? = null,
    @SerialName("carbohydrate_g") val carbohydrateG: Double? = null,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("sugar_g") val sugarG: Double? = null,
    @SerialName("sodium_mg") val sodiumMg: Double? = null
)

@Serializable
data class DataSourceDto(
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("source_version") val sourceVersion: String? = null
)

@Serializable
data class AnonymousAuthRequestDto(
    @SerialName("platform") val platform: String = "android"
)

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AuthResponseDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null
)

@Serializable
data class MealRecommendationsResponseDto(
    @SerialName("remaining_kcal") val remainingKcal: Double? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("recommendations") val recommendations: List<MealRecommendationDto>? = null
)

@Serializable
data class MealRecommendationDto(
    @SerialName("meal_id") val mealId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("serving_weight_g") val servingWeightG: Double? = null,
    @SerialName("nutrition") val nutrition: BasicNutritionDto? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("source") val source: DataSourceDto? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class ProductSearchResponseDto(
    @SerialName("query") val query: String? = null,
    @SerialName("items") val items: List<ProductDto>? = null
)

@Serializable
data class ProductDto(
    @SerialName("product_id") val productId: String? = null,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("serving_size") val servingSizeAmount: Double? = null,
    @SerialName("serving_size_g") val servingSizeG: Double? = null,
    @SerialName("serving_unit") val servingUnit: String? = "g",
    @SerialName("serving_label") val servingLabel: String? = null,
    @SerialName("nutrition_per_serving") val nutritionPerServing: BasicNutritionDto? = null,
    @SerialName("source") val source: DataSourceDto? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class ProfileRequestDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("sex") val sex: String,
    @SerialName("age_years") val ageYears: Int,
    @SerialName("height_cm") val heightCm: Double,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("target_weight_kg") val targetWeightKg: Double,
    @SerialName("weekly_pace_kg") val weeklyPaceKg: Double,
    @SerialName("goal") val goal: String,
    @SerialName("activity_level") val activityLevel: String,
    @SerialName("dietary_preferences") val dietaryPreferences: List<String>,
    @SerialName("allergies") val allergies: List<String>,
    @SerialName("schedules") val schedules: ScheduleDto,
    @SerialName("settings") val settings: ProfileSettingsDto
)

@Serializable
data class ScheduleDto(
    @SerialName("breakfast_time") val breakfastTime: String,
    @SerialName("lunch_time") val lunchTime: String,
    @SerialName("dinner_time") val dinnerTime: String
)

@Serializable
data class ProfileSettingsDto(
    @SerialName("hydration_goal_enabled") val hydrationGoalEnabled: Boolean,
    @SerialName("calorie_warning_enabled") val calorieWarningEnabled: Boolean
)

@Serializable
data class ProfileResponseDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("calculated_targets") val calculatedTargets: CalculatedTargetsDto? = null
)

@Serializable
data class CalculatedTargetsDto(
    @SerialName("bmr_kcal") val bmrKcal: Double? = null,
    @SerialName("tdee_kcal") val tdeeKcal: Double? = null,
    @SerialName("daily_energy_kcal") val dailyEnergyKcal: Double? = null,
    @SerialName("macronutrients") val macronutrients: MacroTargetsDto? = null,
    @SerialName("macro_split_pct") val macroSplitPct: MacroSplitDto? = null,
    @SerialName("water_target_glasses") val waterTargetGlasses: Int? = null
)

@Serializable
data class MacroTargetsDto(
    @SerialName("carbohydrate_g") val carbohydrateG: Double? = null,
    @SerialName("protein_g") val proteinG: Double? = null,
    @SerialName("fat_g") val fatG: Double? = null
)

@Serializable
data class MacroSplitDto(
    @SerialName("carbohydrate") val carbohydrate: Int? = null,
    @SerialName("protein") val protein: Int? = null,
    @SerialName("fat") val fat: Int? = null
)

@Serializable
data class MealLogRequestDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_log_id") val clientLogId: String,
    @SerialName("source") val source: String,
    @SerialName("reference_id") val referenceId: String?,
    @SerialName("meal_name") val mealName: String,
    @SerialName("meal_type") val mealType: String?,
    @SerialName("consumed_weight_g") val consumedWeightG: Double?,
    @SerialName("nutrition") val nutrition: BasicNutritionDto,
    @SerialName("logged_at") val loggedAt: Long
)

@Serializable
data class MealLogResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("log_id") val logId: String? = null,
    @SerialName("client_log_id") val clientLogId: String? = null
) {
    val resolvedId: String? get() = id ?: logId
}

@Serializable
data class WaterLogRequestDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("amount_ml") val amountMl: Int,
    @SerialName("logged_at") val loggedAt: Long
)

@Serializable
data class WaterTodayDto(
    @SerialName("date") val date: String? = null,
    @SerialName("total_ml") val totalMl: Int? = null,
    @SerialName("total_glasses") val totalGlasses: Int? = null,
    @SerialName("target_glasses") val targetGlasses: Int? = null
)