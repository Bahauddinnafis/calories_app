package com.nafis.calories.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Uniform error envelope: `{ "error": { "code", "message", "retryable" } }`. */
data class ApiErrorEnvelopeDto(
    @SerializedName("error") val error: ApiErrorDto? = null
)

data class ApiErrorDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("retryable") val retryable: Boolean? = null
)

data class BasicNutritionDto(
    @SerializedName("energy_kcal") val energyKcal: Double? = null,
    @SerializedName("protein_g") val proteinG: Double? = null,
    @SerializedName("carbohydrate_g") val carbohydrateG: Double? = null,
    @SerializedName("fat_g") val fatG: Double? = null,
    @SerializedName("sugar_g") val sugarG: Double? = null,
    @SerializedName("sodium_mg") val sodiumMg: Double? = null
)

data class DataSourceDto(
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("source_version") val sourceVersion: String? = null
)

// --- Auth (#6) ---

data class AnonymousAuthRequestDto(
    @SerializedName("platform") val platform: String = "android"
)

data class RefreshTokenRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthResponseDto(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

// --- Meal recommendations (#1) ---

data class MealRecommendationsResponseDto(
    @SerializedName("remaining_kcal") val remainingKcal: Double? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("recommendations") val recommendations: List<MealRecommendationDto>? = null
)

data class MealRecommendationDto(
    @SerializedName("meal_id") val mealId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("serving_weight_g") val servingWeightG: Double? = null,
    @SerializedName("nutrition") val nutrition: BasicNutritionDto? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("source") val source: DataSourceDto? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

// --- Packaged products (#2) ---

data class ProductSearchResponseDto(
    @SerializedName("query") val query: String? = null,
    @SerializedName("items") val items: List<ProductDto>? = null
)

data class ProductDto(
    @SerializedName("product_id") val productId: String? = null,
    @SerializedName("barcode") val barcode: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("serving_size") val servingSizeAmount: Double? = null,
    @SerializedName("serving_size_g") val servingSizeG: Double? = null,
    @SerializedName("serving_unit") val servingUnit: String? = "g",
    @SerializedName("serving_label") val servingLabel: String? = null,
    @SerializedName("nutrition_per_serving") val nutritionPerServing: BasicNutritionDto? = null,
    @SerializedName("source") val source: DataSourceDto? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

// --- Profile & targets (#7) ---

data class ProfileRequestDto(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("sex") val sex: String,
    @SerializedName("age_years") val ageYears: Int,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("target_weight_kg") val targetWeightKg: Double,
    @SerializedName("weekly_pace_kg") val weeklyPaceKg: Double,
    @SerializedName("goal") val goal: String,
    @SerializedName("activity_level") val activityLevel: String,
    @SerializedName("dietary_preferences") val dietaryPreferences: List<String>,
    @SerializedName("allergies") val allergies: List<String>,
    @SerializedName("schedules") val schedules: ScheduleDto,
    @SerializedName("settings") val settings: ProfileSettingsDto
)

data class ScheduleDto(
    @SerializedName("breakfast_time") val breakfastTime: String,
    @SerializedName("lunch_time") val lunchTime: String,
    @SerializedName("dinner_time") val dinnerTime: String
)

data class ProfileSettingsDto(
    @SerializedName("hydration_goal_enabled") val hydrationGoalEnabled: Boolean,
    @SerializedName("calorie_warning_enabled") val calorieWarningEnabled: Boolean
)

data class ProfileResponseDto(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("calculated_targets") val calculatedTargets: CalculatedTargetsDto? = null
)

data class CalculatedTargetsDto(
    @SerializedName("bmr_kcal") val bmrKcal: Double? = null,
    @SerializedName("tdee_kcal") val tdeeKcal: Double? = null,
    @SerializedName("daily_energy_kcal") val dailyEnergyKcal: Double? = null,
    @SerializedName("macronutrients") val macronutrients: MacroTargetsDto? = null,
    @SerializedName("macro_split_pct") val macroSplitPct: MacroSplitDto? = null,
    @SerializedName("water_target_glasses") val waterTargetGlasses: Int? = null
)

data class MacroTargetsDto(
    @SerializedName("carbohydrate_g") val carbohydrateG: Double? = null,
    @SerializedName("protein_g") val proteinG: Double? = null,
    @SerializedName("fat_g") val fatG: Double? = null
)

data class MacroSplitDto(
    @SerializedName("carbohydrate") val carbohydrate: Int? = null,
    @SerializedName("protein") val protein: Int? = null,
    @SerializedName("fat") val fat: Int? = null
)

// --- Meal logs (#8) ---

data class MealLogRequestDto(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_log_id") val clientLogId: String,
    @SerializedName("source") val source: String,
    @SerializedName("reference_id") val referenceId: String?,
    @SerializedName("meal_name") val mealName: String,
    @SerializedName("meal_type") val mealType: String?,
    @SerializedName("consumed_weight_g") val consumedWeightG: Double?,
    @SerializedName("nutrition") val nutrition: BasicNutritionDto,
    @SerializedName("logged_at") val loggedAt: Long
)

data class MealLogResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("log_id") val logId: String? = null,
    @SerializedName("client_log_id") val clientLogId: String? = null
) {
    val resolvedId: String? get() = id ?: logId
}

// --- Water logs (#9) ---

data class WaterLogRequestDto(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("amount_ml") val amountMl: Int,
    @SerializedName("logged_at") val loggedAt: Long
)

data class WaterTodayDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("total_ml") val totalMl: Int? = null,
    @SerializedName("total_glasses") val totalGlasses: Int? = null,
    @SerializedName("target_glasses") val targetGlasses: Int? = null
)
