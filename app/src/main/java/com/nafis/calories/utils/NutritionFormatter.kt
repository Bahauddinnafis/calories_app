package com.nafis.calories.utils

import com.nafis.calories.data.model.Nutrition
import java.util.Locale
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

object NutritionFormatter {

    fun formatEnergy(value: Double?): String {
        return value?.roundToInt()?.toString() ?: "-"
    }

    fun formatMacro(value: Double?): String {
        if (value == null) return "-"
        val rounded = value.roundToInt()
        return if (abs(value - rounded) < 0.05) {
            rounded.toString()
        } else {
            String.format("%.1f", value)
        }
    }

    fun formatThousands(value: Double): String =
        String.format(Locale("id", "ID"), "%,d", value.roundToInt())

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

    fun formatFiber(value: Double?): String {
        return formatMacro(value)
    }

    fun formatMineral(value: Double?): String {
        if (value == null) return "-"
        val rounded = value.roundToInt()
        return if (abs(value - rounded) < 0.05) {
            rounded.toString()
        } else {
            String.format("%.1f", value)
        }
    }

    fun formatSugarSdmEquivalent(sugarG: Double?): String {
        if (sugarG == null) return "-"
        val formattedG = formatMacro(sugarG)
        val sdm = sugarG / 12.5
        val sdmFormatted = if (abs(sdm - sdm.roundToInt()) < 0.1) {
            "${sdm.roundToInt()}"
        } else {
            String.format("%.1f", sdm)
        }
        return if (sdm < 0.25) "$formattedG g" else "$formattedG g · ±$sdmFormatted sdm gula"
    }

    fun formatSaltEquivalent(sodiumMg: Double?): String {
        if (sodiumMg == null) return "-"
        val saltG = sodiumMg / 400.0
        val saltFormatted = if (abs(saltG - saltG.roundToInt()) < 0.1) {
            "${saltG.roundToInt()}"
        } else {
            String.format("%.1f", saltG)
        }
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