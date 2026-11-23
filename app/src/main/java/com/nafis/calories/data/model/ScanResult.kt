package com.nafis.calories.data.model

import kotlin.math.abs

/** Domain model representing backend nutrition scan results. */
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
    val displayAiNotice: Boolean = false,
    val aiNoticeText: String? = null,
    val status: ScanStatus = ScanStatus.COMPLETED,
    val errors: List<ScanError> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val consumedWeightG: Double? = null,
    val mealType: MealType? = null,
    val isUserCorrected: Boolean = false
) {
    val defaultConsumedWeightG: Double
        get() = (singleServingWeightG?.takeIf { isMultiUnit } ?: servingWeightG).coerceAtLeast(1.0)

    val confidenceLevel: ConfidenceLevel?
        get() = ConfidenceLevel.from(overallConfidence)

    val isServingUncertain: Boolean
        get() = ConfidenceLevel.from(servingConfidence) == ConfidenceLevel.LOW

    val isMultiUnit: Boolean
        get() = (servingUnitCount ?: 1) > 1 && (singleServingWeightG ?: 0.0) > 0.0

    val isRetryable: Boolean
        get() = errors.isEmpty() || errors.any { it.retryable }
}

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

enum class MealType(val key: String, val label: String) {
    BREAKFAST("breakfast", "Sarapan"),
    LUNCH("lunch", "Makan Siang"),
    DINNER("dinner", "Makan Malam"),
    SNACK("snack", "Camilan");

    companion object {
        fun fromKey(key: String?): MealType? = entries.firstOrNull { it.key == key }

        /** Suggests a meal type from the hour of day (0-23); users can change it afterwards. */
        fun fromHour(hour: Int): MealType = when (hour) {
            in 4..9 -> BREAKFAST
            in 11..14 -> LUNCH
            in 17..21 -> DINNER
            else -> SNACK
        }
    }
}

data class ScanError(
    val stage: String?,
    val code: String?,
    val message: String?,
    val retryable: Boolean
)

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

data class NutritionSource(
    val sourceName: String,
    val sourceVersion: String
)

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

        val count = unitCount ?: return fallback
        val single = singleServingWeightG ?: return fallback
        if (totalWeightG <= 0.0 || count <= 1 || single <= 0.0) return fallback
        if (single > totalWeightG) return fallback

        // Backend rounds single to 0.1 g, so allow that rounding error per unit as well.
        val tolerance = maxOf(totalWeightG * RELATIVE_TOLERANCE, 0.05 * count)
        if (abs(single * count - totalWeightG) > tolerance) return fallback

        return ServingEstimate(
            totalWeightG = totalWeightG,
            singleServingWeightG = single,
            unitName = unitName?.takeIf { it.isNotBlank() } ?: DEFAULT_UNIT_NAME,
            unitCount = count
        )
    }
}