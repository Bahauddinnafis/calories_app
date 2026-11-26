package com.nafis.calories.domain.model

/** Today's remaining budget, sent with a scan so the backend can personalise its nutrition note. */
data class ScanDailyContext(
    val remainingKcal: Double,
    val remainingProteinG: Double,
    val goal: String
)
