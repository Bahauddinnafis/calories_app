package com.nafis.calories.ui.profile

import androidx.lifecycle.ViewModel
import com.nafis.calories.domain.model.DailyTargets
import com.nafis.calories.domain.model.UserProfile
import com.nafis.calories.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(private val repository: UserProfileRepository) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repository.profile
    val dailyTargets: StateFlow<DailyTargets> = repository.dailyTargets

    fun saveProfile(profile: UserProfile) {
        repository.save(profile)
    }
}