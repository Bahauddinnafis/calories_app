package com.nafis.calories.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyTargetCalculatorTest {

    private fun profile(
        sex: Sex = Sex.MALE,
        age: Int = 30,
        height: Double = 170.0,
        weight: Double = 70.0,
        activity: ActivityLevel = ActivityLevel.SEDENTARY,
        goal: WeightGoal = WeightGoal.MAINTAIN
    ) = UserProfile(
        userName = "Test",
        sex = sex,
        ageYears = age,
        heightCm = height,
        weightKg = weight,
        activityLevel = activity,
        goal = goal,
        targetWeightKg = weight
    )

    @Test
    fun noProfileFallsBackToTheDefaultTargets() {
        val targets = DailyTargetCalculator.calculate(null)

        assertEquals(DailyTargetCalculator.DEFAULT_ENERGY_KCAL, targets.energyKcal)
        assertFalse(targets.isPersonalized)
    }

    @Test
    fun maleMaintenanceUsesMifflinStJeorRoundedToTenKcal() {
        // BMR = 700 + 1062.5 - 150 + 5 = 1617.5; x1.2 = 1941 -> 1940
        val targets = DailyTargetCalculator.calculate(profile())

        assertEquals(1940.0, targets.energyKcal)
        assertEquals(0.0, targets.deficitOrSurplusKcal)
        assertTrue(targets.isPersonalized)
    }

    @Test
    fun femaleFormulaSubtracts161() {
        // BMR = 600 + 1000 - 125 - 161 = 1314; x1.55 = 2036.7 -> 2040
        val targets = DailyTargetCalculator.calculate(
            profile(sex = Sex.FEMALE, age = 25, height = 160.0, weight = 60.0, activity = ActivityLevel.MODERATE)
        )

        assertEquals(2040.0, targets.energyKcal)
    }

    @Test
    fun weightLossNeverDropsBelowTheSafetyFloor() {
        // 1941 - 0.5 kg/week x 1100 = 1391 -> male floor 1500
        val male = DailyTargetCalculator.calculate(profile(goal = WeightGoal.LOSE))
        // BMR = 876.5; x1.2 = 1051.8; -550 -> female floor 1200
        val female = DailyTargetCalculator.calculate(
            profile(sex = Sex.FEMALE, age = 60, height = 150.0, weight = 40.0, goal = WeightGoal.LOSE)
        )

        assertEquals(1500.0, male.energyKcal)
        assertEquals(-440.0, male.deficitOrSurplusKcal)
        assertEquals(1200.0, female.energyKcal)
    }

    @Test
    fun macrosFollowThe45Carb30Protein25FatSplit() {
        val targets = DailyTargetCalculator.calculate(null)

        assertEquals(201.0, targets.carbohydrateG)
        assertEquals(134.0, targets.proteinG)
        assertEquals(50.0, targets.fatG)
    }
}
