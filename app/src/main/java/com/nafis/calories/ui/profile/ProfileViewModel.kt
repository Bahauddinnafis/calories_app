package com.nafis.calories.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.data.model.UserProfile
import com.nafis.calories.data.repository.UserProfileRepository
import com.nafis.calories.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(context: Context) : ViewModel() {

    private val repository = UserProfileRepository.getInstance(context)

    val profile: StateFlow<UserProfile?> = repository.profile

    /** Backend-calculated targets when synced, otherwise the on-device formula. */
    val dailyTargets: StateFlow<DailyTargets> = repository.dailyTargets

    fun saveProfile(profile: UserProfile) {
        repository.save(profile)
        CloudSyncManager.requestSync()
    }
}
