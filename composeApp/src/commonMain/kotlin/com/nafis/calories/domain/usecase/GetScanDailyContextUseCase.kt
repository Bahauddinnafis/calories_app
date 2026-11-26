package com.nafis.calories.domain.usecase

import com.nafis.calories.domain.model.ScanDailyContext
import com.nafis.calories.domain.repository.ScanHistoryRepository
import com.nafis.calories.domain.repository.UserProfileRepository
import com.nafis.calories.utils.DailyTotals
import kotlin.time.Clock

class GetScanDailyContextUseCase(
    private val profileRepository: UserProfileRepository,
    private val history: ScanHistoryRepository,
    private val clock: Clock
) {

    suspend operator fun invoke(): ScanDailyContext? {
        val profile = profileRepository.profile.value ?: return null
        val targets = profileRepository.dailyTargets.value
        val consumed = history.getConsumedSince(DailyTotals.startOfDay(clock.now().toEpochMilliseconds()))
        return ScanDailyContext(
            remainingKcal = (targets.energyKcal - consumed.energyKcal).coerceAtLeast(0.0),
            remainingProteinG = (targets.proteinG - consumed.proteinG).coerceAtLeast(0.0),
            goal = profile.goal.key
        )
    }
}