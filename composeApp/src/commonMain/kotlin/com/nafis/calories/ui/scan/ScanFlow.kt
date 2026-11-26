package com.nafis.calories.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.theme.Spacing

@Composable
fun ScanFlow(state: ScanUiState, viewModel: ScanViewModel) {
    when (state) {
        ScanUiState.Home -> Unit
        ScanUiState.Scanner -> MultiModeScannerScreen(
            onImageReady = viewModel::scanFood,
            onBack = viewModel::resetState
        )
        ScanUiState.Loading -> LoadingContent()
        is ScanUiState.Success -> ResultScreen(
            scanResult = state.scanResult,
            capturedImage = state.capturedImage,
            onScanAgain = viewModel::resetState,
            primaryActionText = if (state.fromHistory) "Tutup" else "Simpan ke Catatan",
            onPortionChanged = { viewModel.updateConsumedPortion(state.scanResult.scanId, it) },
            onCorrect = viewModel::startCorrection
        )
        is ScanUiState.Warning -> ResultScreen(
            scanResult = state.scanResult,
            capturedImage = state.capturedImage,
            onScanAgain = viewModel::resetState,
            onPortionChanged = { viewModel.updateConsumedPortion(state.scanResult.scanId, it) },
            onCorrect = viewModel::startCorrection
        )
        is ScanUiState.NutritionUnavailable -> NutritionUnavailableScreen(
            scanResult = state.scanResult,
            capturedImage = state.capturedImage,
            message = state.message,
            canRetry = state.canRetry,
            onRetry = viewModel::retryScan,
            onScanAgain = viewModel::resetState,
            onCorrect = viewModel::startCorrection
        )
        is ScanUiState.Editing -> CorrectionScreen(
            original = state.original,
            isSubmitting = state.isSubmitting,
            errorMessage = state.errorMessage,
            estimateKcal = state.estimateKcal,
            isEstimating = state.isEstimating,
            estimateUnavailable = state.estimateUnavailable,
            onCorrectionChanged = viewModel::previewCorrection,
            onSubmit = viewModel::submitCorrection,
            onCancel = viewModel::cancelCorrection
        )
        is ScanUiState.Error -> ErrorContent(
            message = state.message,
            canRetry = state.canRetry,
            onRetry = viewModel::retryScan,
            onBackHome = viewModel::resetState
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Menghitung kalori…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Biasanya hanya butuh beberapa detik",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analisis belum berhasil",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (canRetry) {
            PrimaryButton(text = "Coba Lagi", onClick = onRetry, icon = Icons.Filled.Refresh)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = "Kembali ke Beranda", onClick = onBackHome)
        } else {
            PrimaryButton(text = "Kembali ke Beranda", onClick = onBackHome)
        }
    }
}