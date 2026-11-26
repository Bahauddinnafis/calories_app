package com.nafis.calories.data.repository

import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.dto.RecalculateRequestDto
import com.nafis.calories.data.remote.dto.ScanResponseDto
import com.nafis.calories.data.remote.mapper.ScanMapper
import com.nafis.calories.domain.model.ScanCorrection
import com.nafis.calories.domain.model.ScanDailyContext
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.repository.FoodAnalysisRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class FoodAnalysisRepositoryImpl(private val api: CaloriesApi) : FoodAnalysisRepository {

    override suspend fun scan(jpegImage: ByteArray, dailyContext: ScanDailyContext?): Result<ScanResult> {
        if (jpegImage.size > MAX_IMAGE_SIZE_BYTES) {
            return Result.failure(Exception("Ukuran gambar terlalu besar. Maksimum 5MB."))
        }
        return request { api.scanFood(jpegImage, dailyContext) }
    }

    override suspend fun recalculate(scanId: String, correction: ScanCorrection): Result<ScanResult> =
        request { api.recalculate(scanId, RecalculateRequestDto.from(correction)) }
            .map { it.copy(isUserCorrected = true) }

    override suspend fun estimate(correction: ScanCorrection): Result<ScanResult> =
        request { api.estimateNutrition(RecalculateRequestDto.from(correction)) }

    private suspend fun request(call: suspend () -> HttpResponse): Result<ScanResult> =
        try {
            val response = call()
            if (response.status.isSuccess()) {
                Result.success(ScanMapper.toDomain(response.body<ScanResponseDto>()))
            } else {
                Result.failure(Exception(messageForStatus(response.status.value)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(messageForException(e)))
        }

    private fun messageForStatus(code: Int): String = when (code) {
        400 -> "Foto tidak dapat dibaca. Coba gunakan foto makanan yang lebih jelas."
        401 -> "Sesi Anda tidak valid. Silakan coba lagi."
        413 -> "Ukuran foto terlalu besar. Gunakan foto di bawah 5 MB."
        422 -> "Data belum lengkap. Pastikan nama makanan, nama bahan, dan beratnya sudah diisi dengan benar."
        429 -> "Terlalu banyak permintaan dalam waktu singkat. Tunggu sebentar, lalu coba lagi."
        in 500..599 -> "Layanan sedang mengalami gangguan. Silakan coba lagi dalam beberapa saat."
        else -> "Terjadi kendala saat menghubungi layanan (kode $code). Silakan coba lagi."
    }

    private fun messageForException(e: Exception): String = when (e) {
        is HttpRequestTimeoutException -> "Analisis memakan waktu terlalu lama. Silakan coba lagi."
        is IOException -> "Tidak dapat terhubung. Periksa koneksi internet Anda, lalu coba lagi."
        else -> "Terjadi kendala saat menganalisis foto. Silakan coba lagi."
    }

    private companion object {
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
    }
}