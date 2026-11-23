package com.nafis.calories.ui.history

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(context: Context) : ViewModel() {

    // Repository dependency - using NEW scan history system
    private val repository = ScanHistoryRepository(context)

    val histories = repository.getAllHistories()

    val historyCount = repository.getHistoryCount()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val imageCache = mutableMapOf<Long, Bitmap>()

    suspend fun getHistoryImage(history: ScanHistoryEntity): Bitmap? {
        // Fast path: Check cache
        imageCache[history.id]?.let { return it }

        // Cache miss: Load from storage
        val bitmap = repository.loadHistoryImage(history.imagePath)

        // Cache for future use
        bitmap?.let {
            imageCache[history.id] = it
        }

        return bitmap
    }

    fun deleteHistory(history: ScanHistoryEntity) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting

            val result = repository.deleteHistory(history)

            _deleteState.value = if (result.isSuccess) {
                // Remove from cache
                imageCache.remove(history.id)
                DeleteState.Success
            } else {
                DeleteState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }

            // Reset state setelah 2 detik (give user time to see message)
            kotlinx.coroutines.delay(2000)
            _deleteState.value = DeleteState.Idle
        }
    }

    fun deleteAllHistories() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting

            val result = repository.deleteAllHistories()

            _deleteState.value = if (result.isSuccess) {
                // Clear image cache
                imageCache.clear()
                val count = result.getOrNull() ?: 0
                DeleteState.Success
            } else {
                DeleteState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }

            // Reset state
            kotlinx.coroutines.delay(2000)
            _deleteState.value = DeleteState.Idle
        }
    }

    fun clearDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        imageCache.clear()
    }
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}