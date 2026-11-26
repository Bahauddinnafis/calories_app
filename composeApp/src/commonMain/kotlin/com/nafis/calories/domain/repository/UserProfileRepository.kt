package com.nafis.calories.domain.repository

import com.nafis.calories.domain.model.DailyTargets
import com.nafis.calories.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserProfileRepository {
    val profile: StateFlow<UserProfile?>
    val dailyTargets: StateFlow<DailyTargets>

    fun save(profile: UserProfile)
}
