package com.nafis.calories.data.service

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.nafis.calories.data.model.ScanCorrection
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.data.remote.api.RetrofitClient
import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import com.nafis.calories.data.remote.dto.ScanResponseDto
import com.nafis.calories.data.remote.mapper.ScanMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Today's remaining budget, sent with a scan so the backend can personalise its nutrition note. */
data class ScanDailyContext(
    val remainingKcal: Double,
    val remainingProteinG: Double,
    val goal: String
)

class FoodAnalysisService {

    private val api = RetrofitClient.api
    private val gson = Gson()

    companion object {
        private const val TAG = "CaloriesPipeline"
        private const val JPEG_QUALITY = 85
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
    }

    suspend fun scanFood(bitmap: Bitmap, dailyContext: ScanDailyContext? = null): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting backend food scan")

            val scaledBitmap = scaleBitmapDown(bitmap, 1536)
            val imageBytes = compressBitmap(scaledBitmap)
            Log.d(TAG, "Image compressed: ${imageBytes.size} bytes")

            if (imageBytes.size > MAX_IMAGE_SIZE_BYTES) {
                return@withContext Result.failure(
                    Exception("Ukuran gambar terlalu besar. Maksimum 5MB.")
                )
            }

            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                "food_image.jpg",
                requestBody
            )

            val textType = "text/plain".toMediaTypeOrNull()
            parseScanResponse(
                api.scanFood(
                    image = imagePart,
                    remainingKcal = dailyContext?.remainingKcal?.toString()?.toRequestBody(textType),
                    remainingProteinG = dailyContext?.remainingProteinG?.toString()?.toRequestBody(textType),
                    goal = dailyContext?.goal?.toRequestBody(textType)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            Result.failure(Exception(handleException(e)))
        }
    }

    suspend fun recalculate(scanId: String, correction: ScanCorrection): Result<ScanResult> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recalculating scan $scanId with ${correction.ingredients.size} ingredients")
                parseScanResponse(api.recalculate(scanId, RecalculateRequestDto.from(correction)))
                    .map { it.copy(isUserCorrected = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Recalculate failed", e)
                Result.failure(Exception(handleException(e)))
            }
        }

    /** Preview of a correction (`POST /api/v1/nutrition/estimate`); nothing is saved. */
    suspend fun estimate(correction: ScanCorrection): Result<ScanResult> = withContext(Dispatchers.IO) {
        try {
            parseScanResponse(api.estimateNutrition(RecalculateRequestDto.from(correction)))
        } catch (e: Exception) {
            Log.w(TAG, "Estimate failed", e)
            Result.failure(Exception(handleException(e)))
        }
    }

    private fun parseScanResponse(response: Response<ResponseBody>): Result<ScanResult> {
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            val errorMessage = response.errorBody()?.string()
                ?: "HTTP ${response.code()}: ${response.message()}"
            Log.e(TAG, "Backend error: $errorMessage")
            return Result.failure(Exception(parseErrorMessage(response.code(), errorMessage)))
        }

        val rawJson = body.string()
        logLongString("RAW Response JSON: $rawJson")

        val scanResult = ScanMapper.toDomain(gson.fromJson(rawJson, ScanResponseDto::class.java))
        Log.d(TAG, "Scan Result: Meal=${scanResult.mealName}, Serving=${scanResult.servingWeightG}g, Confidence=${scanResult.servingConfidence}")
        scanResult.ingredients.forEachIndexed { index, ingredient ->
            Log.d(TAG, "Ingredient ${index + 1}: ${ingredient.foodName}, Weight=${ingredient.estimatedWeightG}g, Status=${ingredient.matchStatus}")
        }
        return Result.success(scanResult)
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int = 1536): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
    }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        return outputStream.toByteArray()
    }

    private fun parseErrorMessage(code: Int, errorBody: String): String {
        return when (code) {
            400 -> "Foto tidak dapat dibaca. Coba gunakan foto makanan yang lebih jelas."
            401 -> "Sesi Anda tidak valid. Silakan coba lagi."
            413 -> "Ukuran foto terlalu besar. Gunakan foto di bawah 5 MB."
            422 -> "Data belum lengkap. Pastikan nama makanan, nama bahan, dan beratnya sudah diisi dengan benar."
            429 -> "Terlalu banyak permintaan dalam waktu singkat. Tunggu sebentar, lalu coba lagi."
            in 500..599 -> "Layanan sedang mengalami gangguan. Silakan coba lagi dalam beberapa saat."
            else -> "Terjadi kendala saat menghubungi layanan (kode $code). Silakan coba lagi."
        }
    }

    private fun handleException(e: Exception): String {
        return when {
            e is UnknownHostException ||
                e.message?.contains("Unable to resolve host") == true ||
                e.message?.contains("Failed to connect") == true ->
                "Tidak dapat terhubung. Periksa koneksi internet Anda, lalu coba lagi."
            e is SocketTimeoutException || e.message?.contains("timeout", ignoreCase = true) == true ->
                "Analisis memakan waktu terlalu lama. Silakan coba lagi."
            e.message?.contains("SSL") == true ->
                "Koneksi tidak aman, sehingga analisis dibatalkan. Silakan coba lagi nanti."
            else ->
                "Terjadi kendala saat menganalisis foto. Silakan coba lagi."
        }
    }

    private fun logLongString(message: String) {
        if (message.length <= 3500) {
            Log.d(TAG, message)
        } else {
            var i = 0
            val length = message.length
            while (i < length) {
                val end = (i + 3500).coerceAtMost(length)
                Log.d(TAG, message.substring(i, end))
                i = end
            }
        }
    }
}