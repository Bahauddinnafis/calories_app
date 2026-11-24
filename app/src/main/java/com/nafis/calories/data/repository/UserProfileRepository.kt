package com.nafis.calories.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.nafis.calories.data.model.ActivityLevel
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.data.model.Sex
import com.nafis.calories.data.model.UserProfile
import com.nafis.calories.data.model.WeightGoal
import com.nafis.calories.data.remote.dto.CalculatedTargetsDto
import com.nafis.calories.data.remote.dto.ProfileRequestDto
import com.nafis.calories.data.remote.dto.ProfileSettingsDto
import com.nafis.calories.data.remote.dto.ScheduleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class UserProfileRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _dailyTargets = MutableStateFlow(computeTargets())
    val dailyTargets: StateFlow<DailyTargets> = _dailyTargets.asStateFlow()

    val isDirty: Boolean get() = prefs.getBoolean(KEY_DIRTY, false)

    fun save(profile: UserProfile) {
        prefs.edit {
            putString(KEY_USER_NAME, profile.userName)
            putString(KEY_SEX, profile.sex.key)
            putInt(KEY_AGE, profile.ageYears)
            putFloat(KEY_HEIGHT, profile.heightCm.toFloat())
            putFloat(KEY_WEIGHT, profile.weightKg.toFloat())
            putString(KEY_ACTIVITY, profile.activityLevel.key)
            putString(KEY_GOAL, profile.goal.key)
            putFloat(KEY_TARGET_WEIGHT, profile.targetWeightKg.toFloat())
            putFloat(KEY_WEEKLY_PACE, profile.weeklyPaceKg.toFloat())
            putStringSet(KEY_DIETARY_PREFS, profile.dietaryPreferences)
            putStringSet(KEY_ALLERGIES, profile.allergies)
            putString(KEY_BREAKFAST_TIME, profile.breakfastTime)
            putString(KEY_LUNCH_TIME, profile.lunchTime)
            putString(KEY_DINNER_TIME, profile.dinnerTime)
            putBoolean(KEY_HYDRATION_ENABLED, profile.hydrationGoalEnabled)
            putBoolean(KEY_WARNING_ENABLED, profile.calorieWarningEnabled)
            putBoolean(KEY_ONBOARDING_COMPLETED, profile.isOnboardingCompleted)
            putBoolean(KEY_DIRTY, true)
            putLong(KEY_REVISION, prefs.getLong(KEY_REVISION, 0L) + 1)
            // Server targets belong to the previous answers; use the local formula until re-synced.
            remove(KEY_SERVER_TARGETS)
        }
        _profile.value = profile
        _dailyTargets.value = computeTargets()
    }

    /** Queues the current profile for upload again, e.g. after signing in to a new account. */
    fun markDirty() {
        prefs.edit {
            putBoolean(KEY_DIRTY, true)
            putLong(KEY_REVISION, prefs.getLong(KEY_REVISION, 0L) + 1)
        }
    }

    /** Local revision, bumped on every save; lets a finished upload tell whether it is still current. */
    val revision: Long get() = prefs.getLong(KEY_REVISION, 0L)

    /** Body for `PUT /api/v1/me/profile`, or null when there is no completed profile yet. */
    fun toRequest(): ProfileRequestDto? {
        val p = _profile.value?.takeIf { it.isOnboardingCompleted } ?: return null
        return ProfileRequestDto(
            displayName = p.userName,
            sex = p.sex.key,
            ageYears = p.ageYears,
            heightCm = p.heightCm,
            weightKg = p.weightKg,
            targetWeightKg = p.targetWeightKg,
            weeklyPaceKg = p.weeklyPaceKg,
            goal = p.goal.key,
            activityLevel = p.activityLevel.key,
            dietaryPreferences = p.dietaryPreferences.sorted(),
            allergies = p.allergies.sorted(),
            schedules = ScheduleDto(p.breakfastTime, p.lunchTime, p.dinnerTime),
            settings = ProfileSettingsDto(p.hydrationGoalEnabled, p.calorieWarningEnabled)
        )
    }

    /**
     * Called after a successful upload of [uploadedRevision]; [targets] are the backend's calculation.
     * Ignored if the user saved again meanwhile, so the newer answers still get uploaded.
     */
    fun onSynced(uploadedRevision: Long, targets: CalculatedTargetsDto?) {
        if (uploadedRevision != revision) return
        prefs.edit {
            putBoolean(KEY_DIRTY, false)
            if (targets?.dailyEnergyKcal != null) putString(KEY_SERVER_TARGETS, gson.toJson(targets))
        }
        _dailyTargets.value = computeTargets()
    }

    /** Stores targets fetched with `GET /me/profile`, unless local edits are still waiting to upload. */
    fun applyServerTargets(targets: CalculatedTargetsDto?) {
        if (isDirty || targets?.dailyEnergyKcal == null) return
        prefs.edit { putString(KEY_SERVER_TARGETS, gson.toJson(targets)) }
        _dailyTargets.value = computeTargets()
    }

    private fun computeTargets(): DailyTargets {
        val local = DailyTargetCalculator.calculate(_profile.value)
        val server = prefs.getString(KEY_SERVER_TARGETS, null)
            ?.let { runCatching { gson.fromJson(it, CalculatedTargetsDto::class.java) }.getOrNull() }
            ?: return local
        val energy = server.dailyEnergyKcal ?: return local
        val tdee = server.tdeeKcal ?: local.tdeeKcal
        return DailyTargets(
            energyKcal = energy,
            proteinG = server.macronutrients?.proteinG ?: local.proteinG,
            carbohydrateG = server.macronutrients?.carbohydrateG ?: local.carbohydrateG,
            fatG = server.macronutrients?.fatG ?: local.fatG,
            tdeeKcal = tdee,
            deficitOrSurplusKcal = ((energy - tdee) / 10.0).roundToInt() * 10.0,
            isPersonalized = true,
            waterTargetGlasses = server.waterTargetGlasses?.takeIf { it > 0 } ?: local.waterTargetGlasses
        )
    }

    private fun read(): UserProfile? {
        val sex = Sex.fromKey(prefs.getString(KEY_SEX, null)) ?: return null
        val activity = ActivityLevel.fromKey(prefs.getString(KEY_ACTIVITY, null)) ?: return null
        val goal = WeightGoal.fromKey(prefs.getString(KEY_GOAL, null)) ?: return null
        val userName = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        if (!prefs.contains(KEY_AGE) || !prefs.contains(KEY_HEIGHT) ||
            !prefs.contains(KEY_WEIGHT) || !prefs.contains(KEY_TARGET_WEIGHT)
        ) return null

        return UserProfile(
            userName = userName,
            sex = sex,
            ageYears = prefs.getInt(KEY_AGE, 0),
            heightCm = prefs.getFloat(KEY_HEIGHT, 0f).toDouble(),
            weightKg = prefs.getFloat(KEY_WEIGHT, 0f).toDouble(),
            activityLevel = activity,
            goal = goal,
            targetWeightKg = prefs.getFloat(KEY_TARGET_WEIGHT, 0f).toDouble(),
            weeklyPaceKg = prefs.getFloat(KEY_WEEKLY_PACE, 0.5f).toDouble(),
            dietaryPreferences = prefs.getStringSet(KEY_DIETARY_PREFS, null)?.toSet() ?: emptySet(),
            allergies = prefs.getStringSet(KEY_ALLERGIES, null)?.toSet() ?: emptySet(),
            breakfastTime = prefs.getString(KEY_BREAKFAST_TIME, null) ?: "07:30",
            lunchTime = prefs.getString(KEY_LUNCH_TIME, null) ?: "12:30",
            dinnerTime = prefs.getString(KEY_DINNER_TIME, null) ?: "19:00",
            hydrationGoalEnabled = prefs.getBoolean(KEY_HYDRATION_ENABLED, true),
            calorieWarningEnabled = prefs.getBoolean(KEY_WARNING_ENABLED, true),
            isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    }

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SEX = "sex"
        private const val KEY_AGE = "age_years"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_ACTIVITY = "activity_level"
        private const val KEY_GOAL = "goal"
        private const val KEY_TARGET_WEIGHT = "target_weight_kg"
        private const val KEY_WEEKLY_PACE = "weekly_pace_kg"
        private const val KEY_DIETARY_PREFS = "dietary_prefs"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_BREAKFAST_TIME = "breakfast_time"
        private const val KEY_LUNCH_TIME = "lunch_time"
        private const val KEY_DINNER_TIME = "dinner_time"
        private const val KEY_HYDRATION_ENABLED = "hydration_enabled"
        private const val KEY_WARNING_ENABLED = "warning_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_DIRTY = "sync_dirty"
        private const val KEY_REVISION = "sync_revision"
        private const val KEY_SERVER_TARGETS = "server_targets_json"

        @Volatile
        private var instance: UserProfileRepository? = null

        fun getInstance(context: Context): UserProfileRepository =
            instance ?: synchronized(this) {
                instance ?: UserProfileRepository(context).also { instance = it }
            }
    }
}