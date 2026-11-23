package com.nafis.calories.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTargetCalculatorTest {

    private fun profile(
        sex: Sex = Sex.MALE,
        age: Int = 30,
        height: Double = 170.0,
        weight: Double = 70.0,
        activity: ActivityLevel = ActivityLevel.SEDENTARY,
        goal: WeightGoal = WeightGoal.MAINTAIN
    ) = UserProfile(sex, age, height, weight, activity, goal)

    @Test
    fun `no profile falls back to generic 2000 kcal`() {
        val targets = DailyTargetCalculator.calculate(null)
        assertEquals(2000.0, targets.energyKcal, 0.0)
        assertFalse(targets.isPersonalized)
    }

    @Test
    fun `male maintenance uses Mifflin-St Jeor rounded to 10 kcal`() {
        // BMR = 700 + 1062.5 - 150 + 5 = 1617.5; x1.2 = 1941 -> 1940
        val targets = DailyTargetCalculator.calculate(profile())
        assertEquals(1940.0, targets.energyKcal, 0.0)
        assertTrue(targets.isPersonalized)
    }

    @Test
    fun `female formula subtracts 161`() {
        // BMR = 600 + 1000 - 125 - 161 = 1314; x1.55 = 2036.7 -> 2040
        val targets = DailyTargetCalculator.calculate(
            profile(sex = Sex.FEMALE, age = 25, height = 160.0, weight = 60.0, activity = ActivityLevel.MODERATE)
        )
        assertEquals(2040.0, targets.energyKcal, 0.0)
    }

    @Test
    fun `weight loss never drops below the safety floor`() {
        // BMR = 400 + 937.5 - 300 - 161 = 876.5; x1.2 = 1051.8; -500 -> floored to 1200
        val targets = DailyTargetCalculator.calculate(
            profile(sex = Sex.FEMALE, age = 60, height = 150.0, weight = 40.0, goal = WeightGoal.LOSE)
        )
        assertEquals(1200.0, targets.energyKcal, 0.0)
    }

    @Test
    fun `macros follow the 15-25-60 energy split`() {
        val targets = DailyTargetCalculator.calculate(null)
        assertEquals(75.0, targets.proteinG, 0.01)
        assertEquals(55.56, targets.fatG, 0.01)
        assertEquals(300.0, targets.carbohydrateG, 0.01)
    }
}
