package com.nafis.calories.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun NutritionUnavailableScreen(
    scanResult: ScanResult,
    capturedImage: Bitmap?,
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onScanAgain: () -> Unit,
    onCorrect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        capturedImage?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = scanResult.mealName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sectionSmall))

            Text(
                text = scanResult.mealName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (scanResult.servingWeightG > 0) {
                Text(
                    text = "Perkiraan berat ${scanResult.servingWeightG.roundToInt()} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sectionMedium))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.componentLarge),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.componentMedium))
                    Column {
                        Text(
                            text = "Kalori belum bisa dihitung",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
                        Text(
                            text = "Hasil ini tidak masuk ke riwayat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (scanResult.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sectionMedium))
                Text(
                    text = "Yang terlihat di foto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.componentSmall))
                scanResult.ingredients.forEach { ingredient ->
                    Text(
                        text = "• ${ingredient.foodName} (±${ingredient.estimatedWeightG.roundToInt()} g)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sectionLarge))

            // Retrying only helps transient failures; otherwise correcting the names is the way forward.
            if (canRetry) {
                PrimaryButton(
                    text = "Coba Lagi",
                    onClick = onRetry,
                    icon = Icons.Filled.Refresh
                )
                Spacer(modifier = Modifier.height(Spacing.componentSmall))
                SecondaryButton(text = "Koreksi Makanan", onClick = onCorrect, icon = Icons.Outlined.Edit)
            } else {
                PrimaryButton(text = "Koreksi Makanan", onClick = onCorrect, icon = Icons.Outlined.Edit)
            }
            Spacer(modifier = Modifier.height(Spacing.componentSmall))
            SecondaryButton(text = "Scan Makanan Lain", onClick = onScanAgain)

            Spacer(modifier = Modifier.height(Spacing.screenBottom))
        }
    }
}