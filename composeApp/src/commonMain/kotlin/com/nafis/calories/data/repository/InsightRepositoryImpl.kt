package com.nafis.calories.data.repository

import com.nafis.calories.core.result.suspendRunCatching
import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.dto.DailyInsightResponseDto
import com.nafis.calories.domain.model.DailyInsight
import com.nafis.calories.domain.model.DailyInsightQuery
import com.nafis.calories.domain.repository.InsightRepository
import io.ktor.client.call.body
import io.ktor.http.isSuccess

class InsightRepositoryImpl(private val api: CaloriesApi) : InsightRepository {

    /** Fails when the backend has nothing to say, so the UI can keep the last real insight. */
    override suspend fun fetchDailyInsight(query: DailyInsightQuery): Result<DailyInsight> = suspendRunCatching {
        val response = api.getDailyInsights(query)
        check(response.status.isSuccess()) { "Daily insight failed: HTTP ${response.status.value}" }
        val insight = response.body<DailyInsightResponseDto>().toDomain()
        check(!insight.headline.isNullOrBlank() || !insight.message.isNullOrBlank()) { "Daily insight is empty" }
        insight
    }

    private fun DailyInsightResponseDto.toDomain() = DailyInsight(
        headline = headline,
        message = message,
        status = status,
        priorityFocus = priorityFocus,
        hydrationNudge = hydrationNudge,
        suggestedCategory = suggestedCategory,
        remainingKcal = remainingKcal,
        remainingProteinG = remainingProteinG
    )
}