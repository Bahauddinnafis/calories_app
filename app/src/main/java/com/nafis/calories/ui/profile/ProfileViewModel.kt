package com.nafis.calories.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.data.model.UserProfile
import com.nafis.calories.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(context: Context) : ViewModel() {

    private val repository = UserProfileRepository.getInstance(context)

    val profile: StateFlow<UserProfile?> = repository.profile

    val dailyTargets: StateFlow<DailyTargets> = repository.profile
        .map { DailyTargetCalculator.calculate(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DailyTargetCalculator.calculate(repository.profile.value)
        )

    fun saveProfile(profile: UserProfile) {
        repository.save(profile)
    }
}