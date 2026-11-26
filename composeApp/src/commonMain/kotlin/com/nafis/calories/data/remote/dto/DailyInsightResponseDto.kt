package com.nafis.calories.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyInsightResponseDto(
    @SerialName("date")
    val date: String? = null,

    @SerialName("time_period")
    val timePeriod: String? = null,

    @SerialName("status")
    val status: String? = null,

    @SerialName("priority_focus")
    val priorityFocus: String? = null,

    @SerialName("headline")
    val headline: String? = null,

    @SerialName("message")
    val message: String? = null,

    @SerialName("remaining_kcal")
    val remainingKcal: Double? = null,

    @SerialName("remaining_protein_g")
    val remainingProteinG: Double? = null,

    @SerialName("hydration_nudge")
    val hydrationNudge: String? = null,

    @SerialName("suggested_category")
    val suggestedCategory: String? = null
)