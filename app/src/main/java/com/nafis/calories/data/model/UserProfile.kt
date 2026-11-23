package com.nafis.calories.data.model

import kotlin.math.roundToInt

enum class Sex(val key: String, val label: String) {
    MALE("male", "Laki-laki"),
    FEMALE("female", "Perempuan");

    companion object {
        fun fromKey(key: String?): Sex? = entries.firstOrNull { it.key == key }
    }
}

enum class ActivityLevel(val key: String, val label: String, val description: String, val factor: Double) {
    SEDENTARY("sedentary", "Jarang bergerak", "Sebagian besar waktu duduk, hampir tidak berolahraga", 1.2),
    LIGHT("light", "Sedikit aktif", "Olahraga ringan 1–3 kali seminggu", 1.375),
    MODERATE("moderate", "Aktif", "Olahraga sedang 3–5 kali seminggu", 1.55),
    HIGH("high", "Sangat aktif", "Olahraga berat hampir setiap hari atau pekerjaan fisik", 1.725);

    companion object {
        fun fromKey(key: String?): ActivityLevel? = entries.firstOrNull { it.key == key }
    }
}

enum class WeightGoal(val key: String, val label: String, val kcalAdjustment: Int) {
    LOSE("lose", "Menurunkan berat badan", -500),
    MAINTAIN("maintain", "Menjaga berat badan", 0),
    GAIN("gain", "Menaikkan berat badan", 300);

    companion object {
        fun fromKey(key: String?): WeightGoal? = entries.firstOrNull { it.key == key }
    }
}

data class UserProfile(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goal: WeightGoal
) {
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
    /** False when no profile exists and generic adult values are used. */
    val isPersonalized: Boolean
)

object DailyTargetCalculator {

    const val DEFAULT_ENERGY_KCAL = 2000.0

    private const val PROTEIN_SHARE = 0.15
    private const val FAT_SHARE = 0.25
    private const val CARB_SHARE = 0.60
    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0
    private const val MIN_KCAL_FEMALE = 1200.0
    private const val MIN_KCAL_MALE = 1500.0

    val default: DailyTargets = fromEnergy(DEFAULT_ENERGY_KCAL, isPersonalized = false)

    fun calculate(profile: UserProfile?): DailyTargets {
        if (profile == null) return default

        val bmr = 10.0 * profile.weightKg +
            6.25 * profile.heightCm -
            5.0 * profile.ageYears +
            if (profile.sex == Sex.MALE) 5.0 else -161.0
        val maintenance = bmr * profile.activityLevel.factor
        val floor = if (profile.sex == Sex.MALE) MIN_KCAL_MALE else MIN_KCAL_FEMALE
        val target = (maintenance + profile.goal.kcalAdjustment).coerceAtLeast(floor)

        val rounded = (target / 10.0).roundToInt() * 10.0
        return fromEnergy(rounded, isPersonalized = true)
    }

    private fun fromEnergy(energyKcal: Double, isPersonalized: Boolean) = DailyTargets(
        energyKcal = energyKcal,
        proteinG = energyKcal * PROTEIN_SHARE / KCAL_PER_G_PROTEIN,
        carbohydrateG = energyKcal * CARB_SHARE / KCAL_PER_G_CARB,
        fatG = energyKcal * FAT_SHARE / KCAL_PER_G_FAT,
        isPersonalized = isPersonalized
    )
}