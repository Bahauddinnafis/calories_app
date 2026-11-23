package com.nafis.calories.data.remote.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Retrofit API interface for Calories backend API endpoints. */
interface CaloriesBackendApi {

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    @Multipart
    @POST("api/v1/scans")
    suspend fun scanFood(
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>

    @POST("api/v1/scans/{scanId}/recalculate")
    suspend fun recalculate(
        @Path("scanId") scanId: String,
        @Body body: RecalculateRequestDto
    ): Response<ResponseBody>
}