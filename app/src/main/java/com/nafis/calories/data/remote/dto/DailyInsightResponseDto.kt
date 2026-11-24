package com.nafis.calories.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DailyInsightResponseDto(
    @SerializedName("date")
    val date: String? = null,

    @SerializedName("time_period")
    val timePeriod: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("priority_focus")
    val priorityFocus: String? = null,

    @SerializedName("headline")
    val headline: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("remaining_kcal")
    val remainingKcal: Double? = null,

    @SerializedName("remaining_protein_g")
    val remainingProteinG: Double? = null,

    @SerializedName("hydration_nudge")
    val hydrationNudge: String? = null,

    @SerializedName("suggested_category")
    val suggestedCategory: String? = null
)