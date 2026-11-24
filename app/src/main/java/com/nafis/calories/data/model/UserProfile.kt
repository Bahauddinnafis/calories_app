package com.nafis.calories.data.model

import kotlin.math.roundToInt

enum class Sex(val key: String, val label: String) {
    MALE("male", "Laki-laki"),
    FEMALE("female", "Perempuan");

    companion object {
        fun fromKey(key: String?): Sex? = entries.firstOrNull { it.key == key }
    }
}

enum class ActivityLevel(
    val key: String,
    val label: String,
    val description: String,
    val factor: Double
) {
    SEDENTARY("sedentary", "Sedentary (Minim Gerak)", "Pekerjaan meja, < 3.000 langkah/hari", 1.2),
    LIGHT("light", "Lightly Active (1–3x / minggu)", "Jalan kaki rutin atau olahraga ringan", 1.375),
    MODERATE("moderate", "Moderately Active (3–5x / minggu)", "Latihan kardio atau beban intensitas sedang", 1.55),
    HIGH("high", "Very Active (6–7x / minggu)", "Latihan atletik berat atau pekerjaan fisik", 1.725);

    companion object {
        fun fromKey(key: String?): ActivityLevel? = entries.firstOrNull { it.key == key }
    }
}

enum class WeightGoal(val key: String, val label: String, val kcalAdjustment: Int) {
    LOSE("lose", "Menurunkan Berat Badan", -500),
    MAINTAIN("maintain", "Menjaga Berat Badan", 0),
    GAIN("gain", "Menaikkan Berat Badan", 300);

    companion object {
        fun fromKey(key: String?): WeightGoal? = entries.firstOrNull { it.key == key }
    }
}

/** Everything here comes from the user's own onboarding answers; there are no placeholder values. */
data class UserProfile(
    val userName: String,
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val goal: WeightGoal = WeightGoal.LOSE,
    val targetWeightKg: Double,
    val weeklyPaceKg: Double = 0.50,
    val dietaryPreferences: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    val breakfastTime: String = "07:30",
    val lunchTime: String = "12:30",
    val dinnerTime: String = "19:00",
    val hydrationGoalEnabled: Boolean = true,
    val calorieWarningEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false
) {
    val avatarInitials: String
        get() {
            val parts = userName.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "P"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "${parts[0].first()}${parts[1].first()}".uppercase()
            }
        }

    companion object {
        val AGE_RANGE = 15..100
        val HEIGHT_RANGE_CM = 100.0..250.0
        val WEIGHT_RANGE_KG = 30.0..300.0
    }
}

data class DailyTargets(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val tdeeKcal: Double = energyKcal,
    val deficitOrSurplusKcal: Double = 0.0,
    /** False when no profile exists and generic adult values are used. */
    val isPersonalized: Boolean = false,
    val waterTargetGlasses: Int = DailyTargetCalculator.DEFAULT_WATER_GLASSES
) {
    /** Share of energy from each macro, derived from the actual gram targets. */
    val carbohydratePct: Int get() = macroPct(carbohydrateG, 4.0)
    val proteinPct: Int get() = macroPct(proteinG, 4.0)
    val fatPct: Int get() = macroPct(fatG, 9.0)

    private fun macroPct(grams: Double, kcalPerGram: Double): Int =
        if (energyKcal > 0) (grams * kcalPerGram / energyKcal * 100).roundToInt() else 0
}

object DailyTargetCalculator {

    const val DEFAULT_ENERGY_KCAL = 1790.0

    /** 8 x 250 ml = 2 L, the general adult guideline; replaced by the backend target when available. */
    const val DEFAULT_WATER_GLASSES = 8

    // Macro Ratio Prototype v3: Carb 45%, Protein 30%, Fat 25%
    private const val CARB_SHARE = 0.45
    private const val PROTEIN_SHARE = 0.30
    private const val FAT_SHARE = 0.25

    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0
    private const val MIN_KCAL_FEMALE = 1200.0
    private const val MIN_KCAL_MALE = 1500.0

    val default: DailyTargets = fromEnergy(
        energyKcal = DEFAULT_ENERGY_KCAL,
        tdeeKcal = 2340.0,
        deficitOrSurplusKcal = -550.0,
        isPersonalized = false
    )

    fun calculate(profile: UserProfile?): DailyTargets {
        if (profile == null) return default

        // Mifflin-St Jeor BMR formula
        val bmr = 10.0 * profile.weightKg +
            6.25 * profile.heightCm -
            5.0 * profile.ageYears +
            if (profile.sex == Sex.MALE) 5.0 else -161.0

        val maintenanceTdee = bmr * profile.activityLevel.factor

        val adjustment = when (profile.goal) {
            WeightGoal.LOSE -> -(profile.weeklyPaceKg * 1100.0) // 0.5kg / week = 550 kcal deficit
            WeightGoal.MAINTAIN -> 0.0
            WeightGoal.GAIN -> (profile.weeklyPaceKg * 700.0)
        }

        val floor = if (profile.sex == Sex.MALE) MIN_KCAL_MALE else MIN_KCAL_FEMALE
        val target = (maintenanceTdee + adjustment).coerceAtLeast(floor)

        val roundedTarget = (target / 10.0).roundToInt() * 10.0
        val roundedTdee = (maintenanceTdee / 10.0).roundToInt() * 10.0
        val roundedDeficit = roundedTarget - roundedTdee

        return fromEnergy(
            energyKcal = roundedTarget,
            tdeeKcal = roundedTdee,
            deficitOrSurplusKcal = roundedDeficit,
            isPersonalized = true
        )
    }

    private fun fromEnergy(
        energyKcal: Double,
        tdeeKcal: Double,
        deficitOrSurplusKcal: Double,
        isPersonalized: Boolean
    ) = DailyTargets(
        energyKcal = energyKcal,
        carbohydrateG = (energyKcal * CARB_SHARE / KCAL_PER_G_CARB).roundToInt().toDouble(),
        proteinG = (energyKcal * PROTEIN_SHARE / KCAL_PER_G_PROTEIN).roundToInt().toDouble(),
        fatG = (energyKcal * FAT_SHARE / KCAL_PER_G_FAT).roundToInt().toDouble(),
        tdeeKcal = tdeeKcal,
        deficitOrSurplusKcal = deficitOrSurplusKcal,
        isPersonalized = isPersonalized
    )
}