package com.nafis.calories.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "scan_history_v2")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanId: String,
    val mealName: String,
    val servingWeightG: Double,
    val singleServingWeightG: Double? = null,
    val servingUnitName: String? = null,
    val servingUnitCount: Int? = null,
    val servingConfidence: Double,
    val ingredientsJson: String,
    val totalEnergyKcal: Double?,
    val totalProteinG: Double?,
    val totalFatG: Double?,
    val totalCarbohydrateG: Double?,
    val totalSugarG: Double? = null,
    val totalSaturatedFatG: Double? = null,
    val totalCholesterolMg: Double? = null,
    val totalFiberG: Double?,
    val totalSodiumMg: Double?,
    val totalCalciumMg: Double?,
    val totalIronMg: Double?,
    val isComplete: Boolean,
    val missingNutrientsJson: String,
    val overallConfidence: Double,
    val uncertaintyNotesJson: String,
    val imagePath: String,
    val timestamp: Long = 0L,
    val consumedWeightG: Double? = null,
    val mealType: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isUserCorrected: Boolean = false,
    val aiNoticeText: String? = null,
    @ColumnInfo(defaultValue = "'[]'")
    val addOnsJson: String = "[]",
    val logSource: String? = null,
    val referenceId: String? = null,
    val remoteId: String? = null,
    @ColumnInfo(defaultValue = "'pending'")
    val syncStatus: String = SYNC_PENDING,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L,
    val nutritionNote: String? = null
) {
    val resolvedLogSource: String
        get() = logSource ?: when {
            scanId.startsWith("rec_") -> SOURCE_RECOMMENDATION
            scanId.startsWith("product_") -> SOURCE_PRODUCT
            scanId.startsWith("manual_") -> SOURCE_MANUAL
            else -> SOURCE_SCAN
        }

    val hasImage: Boolean get() = imagePath.isNotBlank()

    companion object {
        const val SYNC_PENDING = "pending"
        const val SYNC_SYNCED = "synced"

        const val SOURCE_SCAN = "scan"
        const val SOURCE_RECOMMENDATION = "recommendation"
        const val SOURCE_PRODUCT = "product"
        const val SOURCE_MANUAL = "manual"
    }

    val consumedFactor: Double
        get() {
            val consumed = consumedWeightG ?: return 1.0
            return if (servingWeightG > 0 && consumed > 0) consumed / servingWeightG else 1.0
        }

    val consumedEnergyKcal: Double? get() = totalEnergyKcal?.times(consumedFactor)
    val consumedProteinG: Double? get() = totalProteinG?.times(consumedFactor)
    val consumedCarbohydrateG: Double? get() = totalCarbohydrateG?.times(consumedFactor)
    val consumedFatG: Double? get() = totalFatG?.times(consumedFactor)
}

@Serializable
data class IngredientSnapshot(
    val foodCode: String?,
    val foodName: String,
    val estimatedWeightG: Double,
    val matchStatus: String,
    val matchConfidence: Double,
    val energyKcal: Double?,
    val proteinG: Double?,
    val fatG: Double?,
    val carbohydrateG: Double?,
    val sugarG: Double? = null,
    val saturatedFatG: Double? = null,
    val cholesterolMg: Double? = null,
    val fiberG: Double?,
    val sodiumMg: Double?,
    val calciumMg: Double?,
    val ironMg: Double?,
    val sourceName: String?,
    val sourceVersion: String?
)