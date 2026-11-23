package com.nafis.calories.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.nafis.calories.data.local.converter.ScanHistoryConverters

/**
 * Immutable snapshot of scan result.
 * Preserves complete nutrition data as it was at scan time.
 * Future backend changes will not affect historical records.
 * Uses separate table name to avoid conflicts during development.
 */
@Entity(tableName = "scan_history_v2")
@TypeConverters(ScanHistoryConverters::class)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Backend scan ID for reference
    val scanId: String,

    // Meal information
    val mealName: String,

    // Serving information
    val servingWeightG: Double,
    val singleServingWeightG: Double? = null,
    val servingUnitName: String? = null,
    val servingUnitCount: Int? = null,
    val servingConfidence: Double,

    // Ingredients (stored as JSON)
    val ingredientsJson: String,

    // Total nutrition values (nullable - never convert null to zero)
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

    // Completeness flags
    val isComplete: Boolean,
    val missingNutrientsJson: String, // JSON array of missing nutrient names

    // Analysis metadata
    val overallConfidence: Double,
    val uncertaintyNotesJson: String, // JSON array of uncertainty notes

    // Image storage
    val imagePath: String,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis(),

    // User-editable fields (added in DB version 5). Null on older rows.
    /** Portion the user ate; null means the whole detected portion ([servingWeightG]). */
    val consumedWeightG: Double? = null,
    val mealType: String? = null,

    // Added in DB version 6. The default must match the migration's DEFAULT for Room's schema check.
    @ColumnInfo(defaultValue = "0")
    val isUserCorrected: Boolean = false
) {
    /** Factor that turns the stored (whole-photo) totals into what the user actually ate. */
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

/**
 * Ingredient snapshot for history.
 * Stored as part of ingredientsJson.
 */
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
