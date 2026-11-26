package com.nafis.calories.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ScanResponseDto(
    @SerialName("scan_id")
    val scanId: String,

    @SerialName("status")
    val status: String,

    @SerialName("meal_name")
    val mealName: String,

    @SerialName("serving_estimate")
    val servingEstimate: ServingEstimateDto,

    @SerialName("ingredients")
    val ingredients: List<IngredientDto>,

    @SerialName("totals")
    val totals: NutritionTotalsDto,

    @SerialName("analysis")
    val analysis: AnalysisDto,

    @SerialName("nutrition_provenance")
    val nutritionProvenance: NutritionProvenanceDto? = null,

    @SerialName("errors")
    val errors: List<ScanErrorDto>? = null,

    @SerialName("suggested_add_ons")
    val suggestedAddOns: List<SuggestedAddOnDto>? = null,

    @SerialName("nutrition_note")
    val nutritionNote: String? = null,

    @SerialName("ai_notice")
    val aiNotice: String? = null,

    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SuggestedAddOnDto(
    @SerialName("add_on_id")
    val addOnId: String? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("default_weight_g")
    val defaultWeightG: Double? = null,

    @SerialName("energy_kcal")
    val energyKcal: Double? = null
)

@Serializable
data class ScanErrorDto(
    @SerialName("stage")
    val stage: String? = null,

    @SerialName("code")
    val code: String? = null,

    @SerialName("message")
    val message: String? = null,

    @SerialName("retryable")
    val retryable: Boolean? = null
)

@Serializable
data class ServingEstimateDto(
    @SerialName("total_weight_g")
    val totalWeightG: Double,

    @SerialName("single_serving_weight_g")
    val singleServingWeightG: Double? = null,

    @SerialName("serving_unit_name")
    val servingUnitName: String? = null,

    @SerialName("serving_unit_count")
    val servingUnitCount: Int? = null,

    @SerialName("confidence")
    val confidence: Double
)

@Serializable
data class IngredientDto(
    @SerialName("food_code")
    val foodCode: String?,

    @SerialName("food_name")
    val foodName: String,

    @SerialName("estimated_weight_g")
    val estimatedWeightG: Double,

    @SerialName("match_status")
    val matchStatus: String,

    @SerialName("match_confidence")
    val matchConfidence: Double,

    @SerialName("nutrition_source_type")
    val nutritionSourceType: String? = null,

    @SerialName("nutrition_status")
    val nutritionStatus: String? = null,

    @SerialName("nutrition")
    val nutrition: NutritionDto?,

    @SerialName("source")
    val source: NutritionSourceDto?
)

@Serializable
data class NutritionDto(
    @SerialName("energy_kcal")
    val energyKcal: Double?,

    @SerialName("protein_g")
    val proteinG: Double?,

    @SerialName("fat_g")
    val fatG: Double?,

    @SerialName("carbohydrate_g")
    val carbohydrateG: Double?,

    @SerialName("sugar_g")
    val sugarG: Double? = null,

    @SerialName("saturated_fat_g")
    val saturatedFatG: Double? = null,

    @SerialName("cholesterol_mg")
    val cholesterolMg: Double? = null,

    @SerialName("fiber_g")
    val fiberG: Double?,

    @SerialName("sodium_mg")
    val sodiumMg: Double?,

    @SerialName("calcium_mg")
    val calciumMg: Double?,

    @SerialName("iron_mg")
    val ironMg: Double?
)

@Serializable
data class NutritionSourceDto(
    @SerialName("name")
    val name: String? = null,

    @SerialName("source_name")
    val sourceName: String? = null,

    @SerialName("source_version")
    val sourceVersion: String? = null,

    @SerialName("source_food_id")
    val sourceFoodId: String? = null
)

@Serializable
data class NutritionTotalsDto(
    @SerialName("energy_kcal")
    val energyKcal: Double?,

    @SerialName("protein_g")
    val proteinG: Double?,

    @SerialName("fat_g")
    val fatG: Double?,

    @SerialName("carbohydrate_g")
    val carbohydrateG: Double?,

    @SerialName("sugar_g")
    val sugarG: Double? = null,

    @SerialName("saturated_fat_g")
    val saturatedFatG: Double? = null,

    @SerialName("cholesterol_mg")
    val cholesterolMg: Double? = null,

    @SerialName("fiber_g")
    val fiberG: Double?,

    @SerialName("sodium_mg")
    val sodiumMg: Double?,

    @SerialName("calcium_mg")
    val calciumMg: Double?,

    @SerialName("iron_mg")
    val ironMg: Double?,

    @SerialName("complete")
    val complete: Boolean,

    @SerialName("missing_nutrients")
    val missingNutrients: List<String>
)

@Serializable
data class AnalysisDto(
    @SerialName("overall_confidence")
    val overallConfidence: Double,

    @SerialName("uncertainty_notes")
    val uncertaintyNotes: List<String>,

    @SerialName("is_user_corrected")
    val isUserCorrected: Boolean? = null
)

@Serializable
data class NutritionProvenanceDto(
    @SerialName("contains_ai_estimation")
    val containsAiEstimation: Boolean? = null,

    @SerialName("display_ai_notice")
    val displayAiNotice: Boolean? = null,

    @SerialName("ai_notice")
    val aiNotice: JsonElement? = null
) {
    val aiNoticeText: String?
        get() = when {
            aiNotice == null -> null
            aiNotice is JsonPrimitive -> aiNotice.jsonPrimitive.content
            aiNotice is JsonObject -> aiNotice.let { obj ->
                listOf("id", "en").firstNotNullOfOrNull { key ->
                    obj[key]?.takeIf { it is JsonPrimitive }?.jsonPrimitive?.content
                }
            }
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
}