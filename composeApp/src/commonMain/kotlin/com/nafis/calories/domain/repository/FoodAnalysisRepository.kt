package com.nafis.calories.domain.repository

import com.nafis.calories.domain.model.ScanCorrection
import com.nafis.calories.domain.model.ScanDailyContext
import com.nafis.calories.domain.model.ScanResult

interface FoodAnalysisRepository {
    suspend fun scan(jpegImage: ByteArray, dailyContext: ScanDailyContext? = null): Result<ScanResult>
    suspend fun recalculate(scanId: String, correction: ScanCorrection): Result<ScanResult>

    /** Preview of a correction; nothing is saved on the backend. */
    suspend fun estimate(correction: ScanCorrection): Result<ScanResult>
}
