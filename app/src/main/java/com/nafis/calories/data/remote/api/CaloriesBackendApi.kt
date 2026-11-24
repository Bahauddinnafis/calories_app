package com.nafis.calories.data.remote.api

import com.nafis.calories.data.remote.dto.AnonymousAuthRequestDto
import com.nafis.calories.data.remote.dto.AuthResponseDto
import com.nafis.calories.data.remote.dto.MealLogRequestDto
import com.nafis.calories.data.remote.dto.MealLogResponseDto
import com.nafis.calories.data.remote.dto.MealRecommendationsResponseDto
import com.nafis.calories.data.remote.dto.ProductDto
import com.nafis.calories.data.remote.dto.ProductSearchResponseDto
import com.nafis.calories.data.remote.dto.ProfileRequestDto
import com.nafis.calories.data.remote.dto.ProfileResponseDto
import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import com.nafis.calories.data.remote.dto.WaterLogRequestDto
import com.nafis.calories.data.remote.dto.WaterTodayDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit API interface for Calories backend API endpoints. */
interface CaloriesBackendApi {

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    // --- Auth ---

    @POST("api/v1/auth/anonymous")
    suspend fun authAnonymous(@Body body: AnonymousAuthRequestDto = AnonymousAuthRequestDto()): Response<AuthResponseDto>

    // --- Scans ---

    /** Daily context parts are optional; null parts are omitted from the multipart body. */
    @Multipart
    @POST("api/v1/scans")
    suspend fun scanFood(
        @Part image: MultipartBody.Part,
        @Part("remaining_kcal") remainingKcal: RequestBody? = null,
        @Part("remaining_protein_g") remainingProteinG: RequestBody? = null,
        @Part("goal") goal: RequestBody? = null
    ): Response<ResponseBody>

    @POST("api/v1/scans/{scanId}/recalculate")
    suspend fun recalculate(
        @Path("scanId") scanId: String,
        @Body body: RecalculateRequestDto
    ): Response<ResponseBody>

    /** Preview of a correction's nutrition; nothing is saved. Same response shape as a scan. */
    @POST("api/v1/nutrition/estimate")
    suspend fun estimateNutrition(@Body body: RecalculateRequestDto): Response<ResponseBody>

    // --- Insights & recommendations ---

    @GET("api/v1/insights/daily")
    suspend fun getDailyInsights(
        @Query("consumed_kcal") consumedKcal: Double,
        @Query("target_kcal") targetKcal: Double,
        @Query("protein_g") proteinG: Double,
        @Query("target_protein_g") targetProteinG: Double,
        @Query("carbs_g") carbsG: Double,
        @Query("target_carbs_g") targetCarbsG: Double,
        @Query("fat_g") fatG: Double,
        @Query("target_fat_g") targetFatG: Double,
        @Query("water_glasses") waterGlasses: Int,
        @Query("target_glasses") targetGlasses: Int,
        @Query("meal_count") mealCount: Int,
        @Query("goal") goal: String
    ): Response<ResponseBody>

    @GET("api/v1/recommendations/meals")
    suspend fun getMealRecommendations(
        @Query("remaining_kcal") remainingKcal: Double,
        @Query("remaining_protein_g") remainingProteinG: Double,
        @Query("remaining_carbs_g") remainingCarbsG: Double,
        @Query("remaining_fat_g") remainingFatG: Double,
        @Query("meal_type") mealType: String,
        @Query("goal") goal: String,
        @Query("preferences") preferences: String?,
        @Query("allergies") allergies: String?,
        @Query("limit") limit: Int = 4
    ): Response<MealRecommendationsResponseDto>

    // --- Packaged products ---

    @GET("api/v1/products/search")
    suspend fun searchProducts(
        @Query("query") query: String
    ): Response<ProductSearchResponseDto>

    @GET("api/v1/products/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductDto>

    // --- Current user (require Authorization) ---

    @GET("api/v1/me/profile")
    suspend fun getProfile(): Response<ProfileResponseDto>

    @PUT("api/v1/me/profile")
    suspend fun putProfile(@Body body: ProfileRequestDto): Response<ProfileResponseDto>

    @POST("api/v1/me/meal-logs")
    suspend fun createMealLog(@Body body: MealLogRequestDto): Response<MealLogResponseDto>

    @PATCH("api/v1/me/meal-logs/{id}")
    suspend fun updateMealLog(
        @Path("id") id: String,
        @Body body: MealLogRequestDto
    ): Response<MealLogResponseDto>

    @DELETE("api/v1/me/meal-logs/{id}")
    suspend fun deleteMealLog(@Path("id") id: String): Response<Unit>

    @POST("api/v1/me/water-logs")
    suspend fun createWaterLog(@Body body: WaterLogRequestDto): Response<WaterTodayDto>

    @GET("api/v1/me/water-logs/today")
    suspend fun getWaterToday(): Response<WaterTodayDto>
}
