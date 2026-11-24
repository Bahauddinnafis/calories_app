package com.nafis.calories.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** Backend API response DTO for scan endpoint matching API contract. */
data class ScanResponseDto(
    @SerializedName("scan_id")
    val scanId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("meal_name")
    val mealName: String,

    @SerializedName("serving_estimate")
    val servingEstimate: ServingEstimateDto,

    @SerializedName("ingredients")
    val ingredients: List<IngredientDto>,

    @SerializedName("totals")
    val totals: NutritionTotalsDto,

    @SerializedName("analysis")
    val analysis: AnalysisDto,

    @SerializedName("nutrition_provenance")
    val nutritionProvenance: NutritionProvenanceDto? = null,

    @SerializedName("errors")
    val errors: List<ScanErrorDto>? = null,

    @SerializedName("suggested_add_ons")
    val suggestedAddOns: List<SuggestedAddOnDto>? = null,

    /** Personal note about this meal against the user's targets. */
    @SerializedName("nutrition_note")
    val nutritionNote: String? = null,

    /** AI-estimation disclaimer. */
    @SerializedName("ai_notice")
    val aiNotice: String? = null,

    @SerializedName("created_at")
    val createdAt: String
)

data class SuggestedAddOnDto(
    @SerializedName("add_on_id")
    val addOnId: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("default_weight_g")
    val defaultWeightG: Double? = null,

    @SerializedName("energy_kcal")
    val energyKcal: Double? = null
)

data class ScanErrorDto(
    @SerializedName("stage")
    val stage: String? = null,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("retryable")
    val retryable: Boolean? = null
)

data class ServingEstimateDto(
    @SerializedName("total_weight_g")
    val totalWeightG: Double,

    @SerializedName("single_serving_weight_g")
    val singleServingWeightG: Double? = null,

    @SerializedName("serving_unit_name")
    val servingUnitName: String? = null,

    @SerializedName("serving_unit_count")
    val servingUnitCount: Int? = null,

    @SerializedName("confidence")
    val confidence: Double
)

data class IngredientDto(
    @SerializedName("food_code")
    val foodCode: String?,

    @SerializedName("food_name")
    val foodName: String,

    @SerializedName("estimated_weight_g")
    val estimatedWeightG: Double,

    @SerializedName("match_status")
    val matchStatus: String,

    @SerializedName("match_confidence")
    val matchConfidence: Double,

    @SerializedName("nutrition_source_type")
    val nutritionSourceType: String? = null,

    @SerializedName("nutrition_status")
    val nutritionStatus: String? = null,

    @SerializedName("nutrition")
    val nutrition: NutritionDto?,

    @SerializedName("source")
    val source: NutritionSourceDto?
)

data class NutritionDto(
    @SerializedName("energy_kcal")
    val energyKcal: Double?,

    @SerializedName("protein_g")
    val proteinG: Double?,

    @SerializedName("fat_g")
    val fatG: Double?,

    @SerializedName("carbohydrate_g")
    val carbohydrateG: Double?,

    @SerializedName("sugar_g")
    val sugarG: Double? = null,

    @SerializedName("saturated_fat_g")
    val saturatedFatG: Double? = null,

    @SerializedName("cholesterol_mg")
    val cholesterolMg: Double? = null,

    @SerializedName("fiber_g")
    val fiberG: Double?,

    @SerializedName("sodium_mg")
    val sodiumMg: Double?,

    @SerializedName("calcium_mg")
    val calciumMg: Double?,

    @SerializedName("iron_mg")
    val ironMg: Double?
)

data class NutritionSourceDto(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("source_name")
    val sourceName: String? = null,

    @SerializedName("source_version")
    val sourceVersion: String? = null,

    @SerializedName("source_food_id")
    val sourceFoodId: String? = null
)

data class NutritionTotalsDto(
    @SerializedName("energy_kcal")
    val energyKcal: Double?,

    @SerializedName("protein_g")
    val proteinG: Double?,

    @SerializedName("fat_g")
    val fatG: Double?,

    @SerializedName("carbohydrate_g")
    val carbohydrateG: Double?,

    @SerializedName("sugar_g")
    val sugarG: Double? = null,

    @SerializedName("saturated_fat_g")
    val saturatedFatG: Double? = null,

    @SerializedName("cholesterol_mg")
    val cholesterolMg: Double? = null,

    @SerializedName("fiber_g")
    val fiberG: Double?,

    @SerializedName("sodium_mg")
    val sodiumMg: Double?,

    @SerializedName("calcium_mg")
    val calciumMg: Double?,

    @SerializedName("iron_mg")
    val ironMg: Double?,

    @SerializedName("complete")
    val complete: Boolean,

    @SerializedName("missing_nutrients")
    val missingNutrients: List<String>
)

data class AnalysisDto(
    @SerializedName("overall_confidence")
    val overallConfidence: Double,

    @SerializedName("uncertainty_notes")
    val uncertaintyNotes: List<String>,

    @SerializedName("is_user_corrected")
    val isUserCorrected: Boolean? = null
)

data class NutritionProvenanceDto(
    @SerializedName("contains_ai_estimation")
    val containsAiEstimation: Boolean? = null,

    @SerializedName("display_ai_notice")
    val displayAiNotice: Boolean? = null,

    /** Older contract: `{ "id": "...", "en": "..." }`; a plain string is accepted too. */
    @SerializedName("ai_notice")
    val aiNotice: JsonElement? = null
) {
    val aiNoticeText: String?
        get() = when {
            aiNotice == null || aiNotice.isJsonNull -> null
            aiNotice.isJsonPrimitive -> aiNotice.asString
            aiNotice.isJsonObject -> aiNotice.asJsonObject.let { obj ->
                listOf("id", "en").firstNotNullOfOrNull { key ->
                    obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                }
            }
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
}