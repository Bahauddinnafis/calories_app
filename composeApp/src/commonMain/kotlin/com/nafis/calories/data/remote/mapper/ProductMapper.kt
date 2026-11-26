package com.nafis.calories.data.remote.mapper

import com.nafis.calories.data.remote.dto.ProductDto
import com.nafis.calories.domain.model.PackagedProduct
import kotlin.math.roundToInt

object ProductMapper {

    private const val DEFAULT_UNIT = "g"

    fun toDomain(dto: ProductDto): PackagedProduct? {
        val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val nutrition = dto.nutritionPerServing ?: return null
        val kcal = nutrition.energyKcal ?: return null

        val unit = normalizeUnit(dto.servingUnit)
        val amount = (dto.servingSizeAmount ?: dto.servingSizeG)?.takeIf { it > 0.0 }

        return PackagedProduct(
            id = dto.productId?.takeIf { it.isNotBlank() } ?: dto.barcode?.takeIf { it.isNotBlank() } ?: name,
            name = name,
            brand = dto.brand?.trim()?.takeIf { it.isNotEmpty() },
            servingLabel = formatServingLabel(dto.servingLabel, amount, unit),
            servingAmount = amount,
            servingUnit = unit,
            energyKcal = kcal,
            proteinG = nutrition.proteinG ?: 0.0,
            carbohydrateG = nutrition.carbohydrateG ?: 0.0,
            fatG = nutrition.fatG ?: 0.0,
            servingWeightG = amount ?: 0.0
        )
    }

    fun formatServingLabel(label: String?, amount: Double?, unit: String): String? {
        label?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return amount?.let { "1 sajian (${formatAmount(it)} $unit)" }
    }

    private fun normalizeUnit(raw: String?): String = when (raw?.trim()?.lowercase()) {
        null, "" -> DEFAULT_UNIT
        "g", "gr", "gram", "grams" -> "g"
        "ml", "milliliter", "millilitre", "mililiter" -> "ml"
        else -> raw.trim()
    }

    private fun formatAmount(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
    }
}