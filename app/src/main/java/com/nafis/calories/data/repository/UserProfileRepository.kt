package com.nafis.calories.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nafis.calories.data.model.ActivityLevel
import com.nafis.calories.data.model.Sex
import com.nafis.calories.data.model.UserProfile
import com.nafis.calories.data.model.WeightGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the user's profile on the device only. A single instance is shared so every screen
 * observes the same [profile] flow.
 */
class UserProfileRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    fun save(profile: UserProfile) {
        prefs.edit {
            putString(KEY_SEX, profile.sex.key)
            putInt(KEY_AGE, profile.ageYears)
            putFloat(KEY_HEIGHT, profile.heightCm.toFloat())
            putFloat(KEY_WEIGHT, profile.weightKg.toFloat())
            putString(KEY_ACTIVITY, profile.activityLevel.key)
            putString(KEY_GOAL, profile.goal.key)
        }
        _profile.value = profile
    }

    private fun read(): UserProfile? {
        val sex = Sex.fromKey(prefs.getString(KEY_SEX, null)) ?: return null
        val activity = ActivityLevel.fromKey(prefs.getString(KEY_ACTIVITY, null)) ?: return null
        val goal = WeightGoal.fromKey(prefs.getString(KEY_GOAL, null)) ?: return null
        if (!prefs.contains(KEY_AGE) || !prefs.contains(KEY_HEIGHT) || !prefs.contains(KEY_WEIGHT)) return null
        return UserProfile(
            sex = sex,
            ageYears = prefs.getInt(KEY_AGE, 0),
            heightCm = prefs.getFloat(KEY_HEIGHT, 0f).toDouble(),
            weightKg = prefs.getFloat(KEY_WEIGHT, 0f).toDouble(),
            activityLevel = activity,
            goal = goal
        )
    }

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_SEX = "sex"
        private const val KEY_AGE = "age_years"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_ACTIVITY = "activity_level"
        private const val KEY_GOAL = "goal"

        @Volatile
        private var instance: UserProfileRepository? = null

        fun getInstance(context: Context): UserProfileRepository =
            instance ?: synchronized(this) {
                instance ?: UserProfileRepository(context).also { instance = it }
            }
    }
}