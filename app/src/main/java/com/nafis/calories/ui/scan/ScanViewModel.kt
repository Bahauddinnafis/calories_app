package com.nafis.calories.ui.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.model.ScanCorrection
import com.nafis.calories.data.model.ScanCorrectionRules
import com.nafis.calories.data.model.ScanError
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.data.model.ScanStatus
import com.nafis.calories.data.repository.ScanHistoryRepository
import com.nafis.calories.data.service.FoodAnalysisService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Calendar

class ScanViewModel(context: Context) : ViewModel() {

    // Repository dependencies
    private val scanHistoryRepository = ScanHistoryRepository(context)

    // Provider-neutral food analysis service (backend handles AI provider)
    private val foodAnalysisService = FoodAnalysisService()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Home)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastScanImage: Bitmap? = null

    private val pendingSaves = mutableMapOf<String, Job>()
    private var portionUpdateJob: Job? = null

    fun navigateToCamera() {
        _uiState.value = ScanUiState.Camera
    }

    fun navigateToRealTimeScan() {
        _uiState.value = ScanUiState.RealTimeScan
    }

    fun showHistoryDetail(scanResult: ScanResult, image: Bitmap?) {
        _uiState.value = ScanUiState.Success(
            scanResult = scanResult,
            capturedImage = image,
            fromHistory = true
        )
    }

    /** Persists the portion the user ate. Debounced because the slider emits on every drag step. */
    fun updateConsumedPortion(scanId: String, weightG: Double) {
        // Keep the state current too: the correction form pre-fills from the portion eaten.
        updateCurrentResult { it.copy(consumedWeightG = weightG) }
        portionUpdateJob?.cancel()
        portionUpdateJob = viewModelScope.launch {
            delay(PORTION_SAVE_DEBOUNCE_MS)
            pendingSaves[scanId]?.join()
            val updated = scanHistoryRepository.updateConsumedWeight(scanId, weightG)
            if (!updated) Log.w(TAG, "No history row to update portion for scan $scanId")
        }
    }

    fun updateMealType(scanId: String, mealType: MealType) {
        updateCurrentResult { it.copy(mealType = mealType) }
        viewModelScope.launch {
            pendingSaves[scanId]?.join()
            val updated = scanHistoryRepository.updateMealType(scanId, mealType)
            if (!updated) Log.w(TAG, "No history row to update meal type for scan $scanId")
        }
    }

    fun startCorrection() {
        val state = _uiState.value
        val (result, image) = when (state) {
            is ScanUiState.Success -> state.scanResult to state.capturedImage
            is ScanUiState.Warning -> state.scanResult to state.capturedImage
            is ScanUiState.NutritionUnavailable -> state.scanResult to state.capturedImage
            else -> return
        }
        _uiState.value = ScanUiState.Editing(original = result, capturedImage = image, returnTo = state)
    }

    fun cancelCorrection() {
        val state = _uiState.value as? ScanUiState.Editing ?: return
        if (!state.isSubmitting) _uiState.value = state.returnTo
    }

    fun submitCorrection(correction: ScanCorrection) {
        val editing = _uiState.value as? ScanUiState.Editing ?: return
        if (editing.isSubmitting) return

        ScanCorrectionRules.validate(correction)?.let { message ->
            _uiState.value = editing.copy(errorMessage = message)
            return
        }

        val submitting = editing.copy(isSubmitting = true, errorMessage = null)
        _uiState.value = submitting
        // The user may leave (back / bottom bar) while waiting; never pull them back to this flow.
        fun stillWaiting() = _uiState.value == submitting

        viewModelScope.launch {
            val original = editing.original
            val result = foodAnalysisService.recalculate(original.scanId, correction)
            val recalculated = result.getOrElse { error ->
                if (stillWaiting()) _uiState.value = editing.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Perhitungan ulang belum berhasil. Silakan coba lagi."
                )
                return@launch
            }.copy(
                // The corrected weights are what the user ate, so the whole result is the portion.
                consumedWeightG = null,
                mealType = original.mealType,
                timestamp = original.timestamp
            ).let { it.copy(consumedWeightG = it.defaultConsumedWeightG) }

            if (recalculated.status == ScanStatus.NUTRITION_UNAVAILABLE) {
                // Keep the form open so the user can try other names instead of losing their edits.
                if (stillWaiting()) _uiState.value = editing.copy(
                    isSubmitting = false,
                    errorMessage = "Data gizi untuk bahan tersebut belum ditemukan. " +
                        "Coba nama yang lebih umum, misalnya \"nasi putih\" atau \"telur ayam goreng\"."
                )
                return@launch
            }

            pendingSaves[original.scanId]?.join()
            scanHistoryRepository.saveCorrection(recalculated, editing.capturedImage).onFailure {
                Log.e(TAG, "Failed to save corrected scan ${original.scanId}", it)
            }

            if (!stillWaiting()) return@launch
            val fromHistory = (editing.returnTo as? ScanUiState.Success)?.fromHistory == true
            _uiState.value = if (recalculated.status == ScanStatus.PARTIAL) {
                ScanUiState.Warning(recalculated, editing.capturedImage, buildPartialWarning(recalculated))
            } else {
                ScanUiState.Success(recalculated, editing.capturedImage, fromHistory = fromHistory)
            }
        }
    }

    /** Keeps the shown result in sync so recompositions don't revert the user's choice. */
    private fun updateCurrentResult(transform: (ScanResult) -> ScanResult) {
        _uiState.value = when (val state = _uiState.value) {
            is ScanUiState.Success -> state.copy(scanResult = transform(state.scanResult))
            is ScanUiState.Warning -> state.copy(scanResult = transform(state.scanResult))
            else -> state
        }
    }

    fun scanFood(bitmap: Bitmap) {
        lastScanImage = bitmap
        viewModelScope.launch {
            // Step 1: Show loading indicator
            _uiState.value = ScanUiState.Loading

            performScan(bitmap)
        }
    }

    fun scanFoodRealTime(bitmap: Bitmap) {
        lastScanImage = bitmap
        viewModelScope.launch {
            // Don't change state to Loading - keep RealTimeScan state
            // The screen will handle its own loading indicator
            performScan(bitmap)
        }
    }

    private suspend fun performScan(bitmap: Bitmap) {
        try {
            // Step 1: Call backend API for food analysis
            Log.d(TAG, "Sending image to backend for analysis...")
            val result = foodAnalysisService.scanFood(bitmap)

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Backend scan failed: ${error?.message}")
                _uiState.value = ScanUiState.Error(
                    message = error?.message ?: "Analisis belum berhasil. Periksa koneksi internet Anda, lalu coba lagi.",
                    canRetry = true
                )
                return
            }

            val scanResult = result.getOrNull()!!.let {
                it.copy(
                    consumedWeightG = it.defaultConsumedWeightG,
                    mealType = MealType.fromHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                )
            }
            Log.d(TAG, "Backend scan finished: ${scanResult.mealName}, status=${scanResult.status}")
            Log.d(TAG, "Confidence: ${(scanResult.overallConfidence * 100).toInt()}%")
            scanResult.errors.forEach {
                Log.w(TAG, "Backend stage error: stage=${it.stage} code=${it.code} retryable=${it.retryable}")
            }

            // A result without any nutrition is not useful history; let the user retry instead.
            if (scanResult.status == ScanStatus.NUTRITION_UNAVAILABLE) {
                _uiState.value = ScanUiState.NutritionUnavailable(
                    scanResult = scanResult,
                    capturedImage = bitmap,
                    message = describeErrors(scanResult.errors),
                    canRetry = scanResult.isRetryable
                )
                return
            }

            // Step 2: Save to history (asynchronous, best effort)
            pendingSaves[scanResult.scanId] = viewModelScope.launch {
                try {
                    val saveResult = scanHistoryRepository.saveScanHistory(scanResult, bitmap)
                    if (saveResult.isSuccess) {
                        Log.d(TAG, "History saved: ID ${saveResult.getOrNull()}")
                    } else {
                        Log.e(TAG, "Failed to save history", saveResult.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving history", e)
                } finally {
                    pendingSaves.remove(scanResult.scanId)
                }
            }

            // Step 3: Show result
            if (scanResult.status == ScanStatus.PARTIAL) {
                _uiState.value = ScanUiState.Warning(
                    scanResult = scanResult,
                    capturedImage = bitmap,
                    warning = buildPartialWarning(scanResult)
                )
            } else {
                // Complete result
                _uiState.value = ScanUiState.Success(
                    scanResult = scanResult,
                    capturedImage = bitmap
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanFood", e)
            _uiState.value = ScanUiState.Error(
                message = "Terjadi kendala saat menganalisis foto. Silakan coba lagi.",
                canRetry = true
            )
        }
    }

    /** Re-sends the last scanned image. No-op when there is nothing to retry. */
    fun retryScan() {
        val image = lastScanImage ?: return
        scanFood(image)
    }

    private fun buildPartialWarning(scanResult: ScanResult): String {
        val missingIngredients = scanResult.ingredients.count { !it.hasNutrition }
        return if (missingIngredients > 0) {
            "Data gizi untuk $missingIngredients dari ${scanResult.ingredients.size} bahan belum tersedia, " +
                "jadi total kalori di bawah mungkin lebih rendah dari sebenarnya."
        } else {
            "Sebagian data gizi belum tersedia, jadi angka di bawah mungkin belum lengkap."
        }
    }

    private fun describeErrors(errors: List<ScanError>): String {
        val error = errors.firstOrNull()
        return when (error?.code) {
            "provider_timeout" -> "Layanan sedang lambat merespons. Silakan coba lagi."
            "provider_unavailable" -> "Layanan sedang tidak tersedia. Coba lagi dalam beberapa saat."
            "invalid_provider_response" -> "Perhitungan kalori belum berhasil kali ini. Silakan coba lagi."
            // Not retryable per the backend contract, so point to correcting the ingredients instead.
            "estimation_failed" -> "Makanannya sudah dikenali, tetapi kalorinya belum bisa diperkirakan. " +
                "Coba koreksi nama makanan atau bahannya dengan nama yang lebih umum."
            else -> "Makanannya sudah dikenali, tetapi data gizinya belum tersedia."
        }
    }

    fun resetState() {
        lastScanImage = null
        _uiState.value = ScanUiState.Home
    }

    companion object {
        private const val TAG = "ScanViewModel"
        private const val PORTION_SAVE_DEBOUNCE_MS = 400L
    }
}

sealed class ScanUiState {
    data object Home : ScanUiState()
    data object Camera : ScanUiState()
    data object RealTimeScan : ScanUiState()
    data object Loading : ScanUiState()

    data class Success(
        val scanResult: ScanResult,
        val capturedImage: Bitmap?,
        val fromHistory: Boolean = false
    ) : ScanUiState()

    data class Warning(
        val scanResult: ScanResult,
        val capturedImage: Bitmap?,
        val warning: String
    ) : ScanUiState()

    data class NutritionUnavailable(
        val scanResult: ScanResult,
        val capturedImage: Bitmap?,
        val message: String,
        val canRetry: Boolean
    ) : ScanUiState()

    data class Error(
        val message: String,
        val canRetry: Boolean = false
    ) : ScanUiState()

    data class Editing(
        val original: ScanResult,
        val capturedImage: Bitmap?,
        val returnTo: ScanUiState,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null
    ) : ScanUiState()
}