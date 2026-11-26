package com.nafis.calories.data.repository

import com.nafis.calories.core.serialization.AppJson
import com.nafis.calories.core.storage.KeyValueStoreFactory
import com.nafis.calories.data.remote.dto.CalculatedTargetsDto
import com.nafis.calories.data.remote.dto.ProfileRequestDto
import com.nafis.calories.data.remote.dto.ProfileSettingsDto
import com.nafis.calories.data.remote.dto.ScheduleDto
import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.domain.model.ActivityLevel
import com.nafis.calories.domain.model.DailyTargetCalculator
import com.nafis.calories.domain.model.DailyTargets
import com.nafis.calories.domain.model.Sex
import com.nafis.calories.domain.model.UserProfile
import com.nafis.calories.domain.model.WeightGoal
import com.nafis.calories.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class UserProfileRepositoryImpl(
    storeFactory: KeyValueStoreFactory,
    private val syncTrigger: SyncTrigger
) : UserProfileRepository {

    private val store = storeFactory.create(PREFS_NAME)

    private val _profile = MutableStateFlow(read())
    override val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _dailyTargets = MutableStateFlow(computeTargets())
    override val dailyTargets: StateFlow<DailyTargets> = _dailyTargets.asStateFlow()

    val isDirty: Boolean get() = store.getBoolean(KEY_DIRTY, false)

    /** Bumped on every save so a finished upload can tell whether it is still current. */
    val revision: Long get() = store.getLong(KEY_REVISION, 0L)

    override fun save(profile: UserProfile) {
        store.edit {
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
            putLong(KEY_REVISION, revision + 1)
            remove(KEY_SERVER_TARGETS)
        }
        _profile.value = profile
        _dailyTargets.value = computeTargets()
        syncTrigger.requestSync()
    }

    /** Queues the current profile for upload again, e.g. after signing in to a new account. */
    fun markDirty() {
        store.edit {
            putBoolean(KEY_DIRTY, true)
            putLong(KEY_REVISION, revision + 1)
        }
    }

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

    /** Ignored when the user saved again during the upload, so the newer answers still get uploaded. */
    fun onSynced(uploadedRevision: Long, targets: CalculatedTargetsDto?) {
        if (uploadedRevision != revision) return
        store.edit {
            putBoolean(KEY_DIRTY, false)
            if (targets?.dailyEnergyKcal != null) putString(KEY_SERVER_TARGETS, AppJson.encodeToString(targets))
        }
        _dailyTargets.value = computeTargets()
    }

    fun applyServerTargets(targets: CalculatedTargetsDto?) {
        if (isDirty || targets?.dailyEnergyKcal == null) return
        store.edit { putString(KEY_SERVER_TARGETS, AppJson.encodeToString(targets)) }
        _dailyTargets.value = computeTargets()
    }

    private fun computeTargets(): DailyTargets {
        val local = DailyTargetCalculator.calculate(_profile.value)
        val server = store.getString(KEY_SERVER_TARGETS)
            ?.let { runCatching { AppJson.decodeFromString<CalculatedTargetsDto>(it) }.getOrNull() }
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
        val sex = Sex.fromKey(store.getString(KEY_SEX)) ?: return null
        val activity = ActivityLevel.fromKey(store.getString(KEY_ACTIVITY)) ?: return null
        val goal = WeightGoal.fromKey(store.getString(KEY_GOAL)) ?: return null
        val userName = store.getString(KEY_USER_NAME)?.takeIf { it.isNotBlank() } ?: return null
        val requiredKeys = listOf(KEY_AGE, KEY_HEIGHT, KEY_WEIGHT, KEY_TARGET_WEIGHT)
        if (!requiredKeys.all(store::contains)) return null

        return UserProfile(
            userName = userName,
            sex = sex,
            ageYears = store.getInt(KEY_AGE, 0),
            heightCm = store.getFloat(KEY_HEIGHT, 0f).toDouble(),
            weightKg = store.getFloat(KEY_WEIGHT, 0f).toDouble(),
            activityLevel = activity,
            goal = goal,
            targetWeightKg = store.getFloat(KEY_TARGET_WEIGHT, 0f).toDouble(),
            weeklyPaceKg = store.getFloat(KEY_WEEKLY_PACE, 0.5f).toDouble(),
            dietaryPreferences = store.getStringSet(KEY_DIETARY_PREFS).orEmpty(),
            allergies = store.getStringSet(KEY_ALLERGIES).orEmpty(),
            breakfastTime = store.getString(KEY_BREAKFAST_TIME) ?: "07:30",
            lunchTime = store.getString(KEY_LUNCH_TIME) ?: "12:30",
            dinnerTime = store.getString(KEY_DINNER_TIME) ?: "19:00",
            hydrationGoalEnabled = store.getBoolean(KEY_HYDRATION_ENABLED, true),
            calorieWarningEnabled = store.getBoolean(KEY_WARNING_ENABLED, true),
            isOnboardingCompleted = store.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        )
    }

    private companion object {
        const val PREFS_NAME = "user_profile"
        const val KEY_USER_NAME = "user_name"
        const val KEY_SEX = "sex"
        const val KEY_AGE = "age_years"
        const val KEY_HEIGHT = "height_cm"
        const val KEY_WEIGHT = "weight_kg"
        const val KEY_ACTIVITY = "activity_level"
        const val KEY_GOAL = "goal"
        const val KEY_TARGET_WEIGHT = "target_weight_kg"
        const val KEY_WEEKLY_PACE = "weekly_pace_kg"
        const val KEY_DIETARY_PREFS = "dietary_prefs"
        const val KEY_ALLERGIES = "allergies"
        const val KEY_BREAKFAST_TIME = "breakfast_time"
        const val KEY_LUNCH_TIME = "lunch_time"
        const val KEY_DINNER_TIME = "dinner_time"
        const val KEY_HYDRATION_ENABLED = "hydration_enabled"
        const val KEY_WARNING_ENABLED = "warning_enabled"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_DIRTY = "sync_dirty"
        const val KEY_REVISION = "sync_revision"
        const val KEY_SERVER_TARGETS = "server_targets_json"
    }
}
