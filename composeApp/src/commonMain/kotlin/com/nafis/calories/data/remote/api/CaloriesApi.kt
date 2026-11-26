package com.nafis.calories.data.remote.api

import com.nafis.calories.data.remote.dto.AnonymousAuthRequestDto
import com.nafis.calories.data.remote.dto.MealLogRequestDto
import com.nafis.calories.data.remote.dto.ProfileRequestDto
import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import com.nafis.calories.data.remote.dto.WaterLogRequestDto
import com.nafis.calories.domain.model.DailyInsightQuery
import com.nafis.calories.domain.model.ScanDailyContext
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class CaloriesApi(private val client: HttpClient) {

    suspend fun authAnonymous(platform: String): HttpResponse =
        client.post("api/v1/auth/anonymous") { json(AnonymousAuthRequestDto(platform)) }

    /** Forces the next request to read the tokens again after the session changed outside Ktor. */
    fun reloadTokens() {
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }

    suspend fun scanFood(jpegImage: ByteArray, dailyContext: ScanDailyContext?): HttpResponse =
        client.submitFormWithBinaryData(
            url = "api/v1/scans",
            formData = formData {
                append("image", jpegImage, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"food_image.jpg\"")
                })
                dailyContext?.let {
                    append("remaining_kcal", it.remainingKcal.toString())
                    append("remaining_protein_g", it.remainingProteinG.toString())
                    append("goal", it.goal)
                }
            }
        )

    suspend fun recalculate(scanId: String, body: RecalculateRequestDto): HttpResponse =
        client.post("api/v1/scans/$scanId/recalculate") { json(body) }

    suspend fun estimateNutrition(body: RecalculateRequestDto): HttpResponse =
        client.post("api/v1/nutrition/estimate") { json(body) }

    suspend fun getDailyInsights(query: DailyInsightQuery): HttpResponse =
        client.get("api/v1/insights/daily") {
            parameter("consumed_kcal", query.consumedKcal)
            parameter("target_kcal", query.targetKcal)
            parameter("protein_g", query.proteinG)
            parameter("target_protein_g", query.targetProteinG)
            parameter("carbs_g", query.carbohydrateG)
            parameter("target_carbs_g", query.targetCarbohydrateG)
            parameter("fat_g", query.fatG)
            parameter("target_fat_g", query.targetFatG)
            parameter("water_glasses", query.waterGlasses)
            parameter("target_glasses", query.targetGlasses)
            parameter("meal_count", query.mealCount)
            parameter("goal", query.goal.key)
        }

    suspend fun getMealRecommendations(
        remainingKcal: Double,
        remainingProteinG: Double,
        remainingCarbsG: Double,
        remainingFatG: Double,
        mealType: String,
        goal: String,
        preferences: String?,
        allergies: String?,
        limit: Int = 4
    ): HttpResponse = client.get("api/v1/recommendations/meals") {
        parameter("remaining_kcal", remainingKcal)
        parameter("remaining_protein_g", remainingProteinG)
        parameter("remaining_carbs_g", remainingCarbsG)
        parameter("remaining_fat_g", remainingFatG)
        parameter("meal_type", mealType)
        parameter("goal", goal)
        parameter("preferences", preferences)
        parameter("allergies", allergies)
        parameter("limit", limit)
    }

    suspend fun searchProducts(query: String): HttpResponse =
        client.get("api/v1/products/search") { parameter("query", query) }

    suspend fun getProductByBarcode(barcode: String): HttpResponse =
        client.get("api/v1/products/barcode/$barcode")

    suspend fun getProfile(): HttpResponse = client.get("api/v1/me/profile")

    suspend fun putProfile(body: ProfileRequestDto): HttpResponse =
        client.put("api/v1/me/profile") { json(body) }

    suspend fun createMealLog(body: MealLogRequestDto): HttpResponse =
        client.post("api/v1/me/meal-logs") { json(body) }

    suspend fun updateMealLog(remoteId: String, body: MealLogRequestDto): HttpResponse =
        client.patch("api/v1/me/meal-logs/$remoteId") { json(body) }

    suspend fun deleteMealLog(remoteId: String): HttpResponse =
        client.delete("api/v1/me/meal-logs/$remoteId")

    suspend fun createWaterLog(body: WaterLogRequestDto): HttpResponse =
        client.post("api/v1/me/water-logs") { json(body) }

    suspend fun getWaterToday(): HttpResponse = client.get("api/v1/me/water-logs/today")

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
