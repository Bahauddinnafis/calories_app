package com.nafis.calories.data.repository

import com.nafis.calories.data.remote.dto.CalculatedTargetsDto
import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.domain.model.Sex
import com.nafis.calories.domain.model.UserProfile
import com.nafis.calories.testing.FakeKeyValueStoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserProfileRepositoryImplTest {

    private val storeFactory = FakeKeyValueStoreFactory()

    private fun repository() = UserProfileRepositoryImpl(storeFactory, SyncTrigger())

    private val profile = UserProfile(
        userName = "Nafis",
        sex = Sex.MALE,
        ageYears = 28,
        heightCm = 170.0,
        weightKg = 70.0,
        targetWeightKg = 65.0,
        dietaryPreferences = setOf("halal"),
        isOnboardingCompleted = true
    )

    @Test
    fun readsProfileStoredWithTheLegacyKeys() {
        repository().save(profile)

        val reloaded = repository().profile.value

        assertEquals(profile, reloaded)
    }

    @Test
    fun staleUploadKeepsTheProfileDirty() {
        val repository = repository()
        repository.save(profile)
        val uploadedRevision = repository.revision
        repository.save(profile.copy(weightKg = 69.0))

        repository.onSynced(uploadedRevision, targets = null)

        assertTrue(repository.isDirty)
    }

    @Test
    fun currentUploadAppliesServerTargets() {
        val repository = repository()
        repository.save(profile)

        repository.onSynced(repository.revision, CalculatedTargetsDto(dailyEnergyKcal = 1_900.0, tdeeKcal = 2_400.0))

        assertFalse(repository.isDirty)
        assertEquals(1_900.0, repository.dailyTargets.value.energyKcal)
        assertTrue(repository.dailyTargets.value.isPersonalized)
    }
}