package com.nafis.calories.utils

import com.nafis.calories.domain.model.Nutrition
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NutrientLevel(
    val label: String,
    val description: String
) {
    LOW("Rendah", "Masih jauh di bawah batas harian"),
    MODERATE("Sedang", "Wajar, perhatikan asupan lain hari ini"),
    HIGH("Tinggi", "Cukup besar dari batas harian, sebaiknya dibatasi")
}

fun Double?.formatKcalInt(): Int = this?.roundToInt() ?: 0

object NutritionFormatter {

    fun formatEnergy(value: Double?): String {
        return value.formatKcalInt().toString()
    }

    fun formatMacro(value: Double?): String {
        if (value == null) return "-"
        val rounded = value.roundToInt()
        return if (abs(value - rounded) < 0.05) rounded.toString() else formatDecimal(value)
    }

    fun formatThousands(value: Double): String {
        val rounded = value.roundToInt()
        val digits = abs(rounded).toString().reversed().chunked(3).joinToString(".").reversed()
        return if (rounded < 0) "-$digits" else digits
    }

    /** One decimal with the Indonesian comma, e.g. 1,5. */
    fun formatDecimal(value: Double): String {
        val tenths = (value * 10).roundToInt()
        val sign = if (tenths < 0) "-" else ""
        return "$sign${abs(tenths) / 10},${abs(tenths) % 10}"
    }

    fun missingNutrientLabels(nutrition: Nutrition): List<String> = listOfNotNull(
        "kalori".takeIf { nutrition.energyKcal == null },
        "protein".takeIf { nutrition.proteinG == null },
        "karbohidrat".takeIf { nutrition.carbohydrateG == null },
        "lemak".takeIf { nutrition.fatG == null },
        "gula".takeIf { nutrition.sugarG == null },
        "garam".takeIf { nutrition.sodiumMg == null },
        "lemak jenuh".takeIf { nutrition.saturatedFatG == null },
        "kolesterol".takeIf { nutrition.cholesterolMg == null },
        "serat".takeIf { nutrition.fiberG == null }
    )

    fun formatFiber(value: Double?): String = formatMacro(value)

    fun formatMineral(value: Double?): String = formatMacro(value)

    fun formatSugarSdmEquivalent(sugarG: Double?): String {
        if (sugarG == null) return "-"
        val formattedG = formatMacro(sugarG)
        val sdm = sugarG / 12.5
        val sdmFormatted = if (abs(sdm - sdm.roundToInt()) < 0.1) "${sdm.roundToInt()}" else formatDecimal(sdm)
        return if (sdm < 0.25) "$formattedG g" else "$formattedG g · ±$sdmFormatted sdm gula"
    }

    fun formatSaltEquivalent(sodiumMg: Double?): String {
        if (sodiumMg == null) return "-"
        val saltG = sodiumMg / 400.0
        val saltFormatted = if (abs(saltG - saltG.roundToInt()) < 0.1) "${saltG.roundToInt()}" else formatDecimal(saltG)
        return if (saltG < 0.1) {
            "${formatMineral(sodiumMg)} mg"
        } else {
            "${formatMineral(sodiumMg)} mg · setara ±$saltFormatted g garam"
        }
    }

    fun getSugarLevel(sugarG: Double?): NutrientLevel? {
        val value = sugarG ?: return null
        return when {
            value < 5.0 -> NutrientLevel.LOW
            value <= 15.0 -> NutrientLevel.MODERATE
            else -> NutrientLevel.HIGH
        }
    }

    fun getSaturatedFatLevel(saturatedFatG: Double?): NutrientLevel? {
        val value = saturatedFatG ?: return null
        return when {
            value < 3.0 -> NutrientLevel.LOW
            value <= 7.0 -> NutrientLevel.MODERATE
            else -> NutrientLevel.HIGH
        }
    }

    fun getSodiumLevel(sodiumMg: Double?): NutrientLevel? {
        val value = sodiumMg ?: return null
        return when {
            value < 300.0 -> NutrientLevel.LOW
            value <= 700.0 -> NutrientLevel.MODERATE
            else -> NutrientLevel.HIGH
        }
    }

    fun getCholesterolLevel(cholesterolMg: Double?): NutrientLevel? {
        val value = cholesterolMg ?: return null
        return when {
            value < 50.0 -> NutrientLevel.LOW
            value <= 150.0 -> NutrientLevel.MODERATE
            else -> NutrientLevel.HIGH
        }
    }
}