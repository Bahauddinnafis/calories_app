package com.nafis.calories.data.repository

import android.util.Log
import com.google.gson.Gson
import com.nafis.calories.data.remote.api.RetrofitClient
import com.nafis.calories.data.remote.dto.ApiErrorEnvelopeDto
import com.nafis.calories.data.remote.mapper.ProductMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/** A packaged food/drink product with nutrition per serving (as printed on the label). */
data class PackagedProduct(
    val id: String,
    val name: String,
    val brand: String?,
    /** Ready to display, e.g. "1 kotak (200 ml)" or "1 sajian (100 g)"; null when unknown. */
    val servingLabel: String?,
    val servingAmount: Double?,
    /** "g" or "ml". */
    val servingUnit: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val servingWeightG: Double
)

/**
 * Looks up packaged products by name (`GET /api/v1/products/search`) or barcode
 * (`GET /api/v1/products/barcode/{barcode}`).
 */
object PackagedProductRepository {

    private const val TAG = "PackagedProductRepo"
    private const val ERROR_NOT_FOUND = "product_not_found"
    private val BARCODE_REGEX = Regex("^\\d{8,14}$")
    private val gson = Gson()

    fun isBarcode(input: String): Boolean = BARCODE_REGEX.matches(input.trim())

    suspend fun search(input: String): Result<List<PackagedProduct>> = withContext(Dispatchers.IO) {
        val query = input.trim()
        if (query.length < 2) return@withContext Result.success(emptyList())
        try {
            if (isBarcode(query)) {
                val response = RetrofitClient.api.getProductByBarcode(query)
                val product = response.body()
                if (response.isSuccessful && product != null) {
                    Result.success(listOfNotNull(ProductMapper.toDomain(product)))
                } else {
                    errorResult(response)
                }
            } else {
                val response = RetrofitClient.api.searchProducts(query)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(body.items.orEmpty().mapNotNull(ProductMapper::toDomain))
                } else {
                    errorResult(response)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Product lookup error", e)
            Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet kamu, lalu coba lagi."))
        }
    }

    /**
     * `product_not_found` (or 404) is an empty result, not an error. Otherwise the backend's
     * user-facing message is shown when it sends one. The error body can only be read once.
     */
    private fun errorResult(response: Response<*>): Result<List<PackagedProduct>> {
        val error = parseError(response)
        if (response.code() == 404 || error?.code == ERROR_NOT_FOUND) return Result.success(emptyList())
        Log.w(TAG, "Product lookup failed: HTTP ${response.code()} code=${error?.code}")
        return Result.failure(
            Exception(error?.message?.takeIf { it.isNotBlank() } ?: "Pencarian produk belum berhasil. Silakan coba lagi.")
        )
    }

    private fun parseError(response: Response<*>) = runCatching {
        response.errorBody()?.charStream()?.use { gson.fromJson(it, ApiErrorEnvelopeDto::class.java)?.error }
    }.getOrNull()
}