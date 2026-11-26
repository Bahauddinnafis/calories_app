package com.nafis.calories.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock

fun Double?.formatKcalInt(): Int = this?.roundToInt() ?: 0

@Serializable
data class ScanResult(
    val scanId: String,
    val mealName: String,
    val servingWeightG: Double,
    val singleServingWeightG: Double? = null,
    val servingUnitName: String? = null,
    val servingUnitCount: Int? = null,
    val servingConfidence: Double,
    val ingredients: List<Ingredient>,
    val totalNutrition: Nutrition,
    val isComplete: Boolean,
    val missingNutrients: List<String>,
    val overallConfidence: Double,
    val uncertaintyNotes: List<String>,
    val personalNutritionNote: String? = null,
    val aiDisclaimerText: String? = null,
    val status: ScanStatus = ScanStatus.COMPLETED,
    val errors: List<ScanError> = emptyList(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val consumedWeightG: Double? = null,
    val mealType: MealType? = null,
    val isUserCorrected: Boolean = false,
    val suggestedAddOns: List<SuggestedAddOn> = emptyList()
) {
    val defaultConsumedWeightG: Double
        get() {
            if (singleServingWeightG != null && singleServingWeightG > 0.0) {
                return singleServingWeightG
            }
            return if (servingWeightG > 250.0) {
                (servingWeightG / 5.0).coerceIn(80.0, 150.0)
            } else {
                servingWeightG.coerceAtLeast(1.0)
            }
        }

    val effectiveConsumedWeightG: Double
        get() = consumedWeightG ?: defaultConsumedWeightG

    val effectiveConsumedNutrition: Nutrition
        get() {
            val baseFull = servingWeightG.takeIf { it > 0.0 } ?: defaultConsumedWeightG
            val ratio = (effectiveConsumedWeightG / baseFull).coerceIn(0.05, 5.0)
            return totalNutrition.scaled(ratio)
        }

    val confidenceLevel: ConfidenceLevel?
        get() = ConfidenceLevel.from(overallConfidence)

    val isServingUncertain: Boolean
        get() = ConfidenceLevel.from(servingConfidence) == ConfidenceLevel.LOW

    val isMultiUnit: Boolean
        get() = (servingUnitCount ?: 1) > 1 && (singleServingWeightG ?: 0.0) > 0.0

    val isRetryable: Boolean
        get() = errors.isEmpty() || errors.any { it.retryable }
}

@Serializable
enum class ScanStatus {
    COMPLETED,
    PARTIAL,
    NUTRITION_UNAVAILABLE;

    companion object {
        fun fromApi(value: String?): ScanStatus? = when (value?.lowercase()) {
            "completed" -> COMPLETED
            "partial" -> PARTIAL
            "nutrition_unavailable" -> NUTRITION_UNAVAILABLE
            else -> null
        }

        fun resolve(
            apiStatus: String?,
            ingredients: List<Ingredient>,
            totals: Nutrition,
            totalsComplete: Boolean
        ): ScanStatus {
            val derived = when {
                !totals.hasAnyValue() && ingredients.none { it.hasNutrition } -> NUTRITION_UNAVAILABLE
                !totalsComplete || ingredients.any { !it.hasNutrition } -> PARTIAL
                else -> COMPLETED
            }
            val reported = fromApi(apiStatus) ?: derived
            return maxOf(reported, derived)
        }
    }
}

@Serializable
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        const val HIGH_THRESHOLD = 0.75
        const val MEDIUM_THRESHOLD = 0.5

        fun from(score: Double): ConfidenceLevel? = when {
            score.isNaN() || score <= 0.0 -> null
            score >= HIGH_THRESHOLD -> HIGH
            score >= MEDIUM_THRESHOLD -> MEDIUM
            else -> LOW
        }
    }
}

@Serializable
enum class MealType(val key: String, val label: String) {
    BREAKFAST("breakfast", "Sarapan"),
    LUNCH("lunch", "Makan Siang"),
    DINNER("dinner", "Makan Malam"),
    SNACK("snack", "Camilan");

    companion object {
        fun fromKey(key: String?): MealType? = entries.firstOrNull { it.key == key }

        fun fromHour(hour: Int): MealType = when (hour) {
            in 4..9 -> BREAKFAST
            in 11..14 -> LUNCH
            in 17..21 -> DINNER
            else -> SNACK
        }
    }
}

object ScanNotes {
    const val DEFAULT_AI_DISCLAIMER = "Estimasi AI — sesuaikan porsi jika diperlukan."

    fun personalNote(nutritionNote: String?, uncertaintyNotes: List<String>): String? =
        nutritionNote?.trim()?.takeIf { it.isNotEmpty() }
            ?: uncertaintyNotes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                .joinToString(" ").takeIf { it.isNotEmpty() }

    fun aiDisclaimer(aiNotice: String?): String =
        aiNotice?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_AI_DISCLAIMER
}

@Serializable
data class SuggestedAddOn(
    val addOnId: String,
    val name: String,
    val defaultWeightG: Double,
    val energyKcal: Double?
)

@Serializable
data class ScanError(
    val stage: String?,
    val code: String?,
    val message: String?,
    val retryable: Boolean
)

@Serializable
data class Ingredient(
    val foodCode: String?,
    val foodName: String,
    val estimatedWeightG: Double,
    val matchStatus: String,
    val matchConfidence: Double,
    val nutritionSourceType: String? = null,
    val nutritionStatus: String? = null,
    val nutrition: Nutrition?,
    val source: NutritionSource?
) {
    val hasNutrition: Boolean
        get() = nutritionStatus != "unavailable" && nutrition?.hasAnyValue() == true
}

@Serializable
data class Nutrition(
    val energyKcal: Double?,
    val proteinG: Double?,
    val fatG: Double?,
    val carbohydrateG: Double?,
    val sugarG: Double? = null,
    val saturatedFatG: Double? = null,
    val cholesterolMg: Double? = null,
    val fiberG: Double?,
    val sodiumMg: Double?,
    val calciumMg: Double?,
    val ironMg: Double?
) {
    fun hasAnyValue(): Boolean = listOf(
        energyKcal, proteinG, fatG, carbohydrateG, sugarG, saturatedFatG,
        cholesterolMg, fiberG, sodiumMg, calciumMg, ironMg
    ).any { it != null }

    fun scaled(factor: Double) = Nutrition(
        energyKcal = energyKcal?.times(factor),
        proteinG = proteinG?.times(factor),
        fatG = fatG?.times(factor),
        carbohydrateG = carbohydrateG?.times(factor),
        sugarG = sugarG?.times(factor),
        saturatedFatG = saturatedFatG?.times(factor),
        cholesterolMg = cholesterolMg?.times(factor),
        fiberG = fiberG?.times(factor),
        sodiumMg = sodiumMg?.times(factor),
        calciumMg = calciumMg?.times(factor),
        ironMg = ironMg?.times(factor)
    )

    companion object {
        fun empty() = Nutrition(
            energyKcal = null,
            proteinG = null,
            fatG = null,
            carbohydrateG = null,
            sugarG = null,
            saturatedFatG = null,
            cholesterolMg = null,
            fiberG = null,
            sodiumMg = null,
            calciumMg = null,
            ironMg = null
        )
    }
}

@Serializable
data class NutritionSource(
    val sourceName: String,
    val sourceVersion: String
)

@Serializable
data class ServingEstimate(
    val totalWeightG: Double,
    val singleServingWeightG: Double,
    val unitName: String,
    val unitCount: Int
)

object ServingNormalizer {
    const val DEFAULT_UNIT_NAME = "porsi"
    private const val RELATIVE_TOLERANCE = 0.02

    fun normalize(
        totalWeightG: Double,
        singleServingWeightG: Double?,
        unitName: String?,
        unitCount: Int?
    ): ServingEstimate {
        val fallback = ServingEstimate(
            totalWeightG = totalWeightG,
            singleServingWeightG = totalWeightG,
            unitName = unitName?.takeIf { it.isNotBlank() && (unitCount ?: 1) <= 1 } ?: DEFAULT_UNIT_NAME,
            unitCount = 1
        )

        if (singleServingWeightG == null || singleServingWeightG <= 0.0) {
            return fallback
        }

        val count = unitCount ?: kotlin.math.round(totalWeightG / singleServingWeightG).toInt().coerceAtLeast(1)
        val expectedTotal = singleServingWeightG * count
        if (abs(expectedTotal - totalWeightG) / totalWeightG <= RELATIVE_TOLERANCE) {
            return ServingEstimate(
                totalWeightG = totalWeightG,
                singleServingWeightG = singleServingWeightG,
                unitName = unitName?.takeIf { it.isNotBlank() } ?: DEFAULT_UNIT_NAME,
                unitCount = count
            )
        }

        return fallback
    }
}