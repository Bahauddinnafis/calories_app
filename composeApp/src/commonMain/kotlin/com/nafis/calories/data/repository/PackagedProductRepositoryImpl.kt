package com.nafis.calories.data.repository

import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.dto.ApiErrorEnvelopeDto
import com.nafis.calories.data.remote.dto.ProductDto
import com.nafis.calories.data.remote.dto.ProductSearchResponseDto
import com.nafis.calories.data.remote.mapper.ProductMapper
import com.nafis.calories.domain.model.PackagedProduct
import com.nafis.calories.domain.repository.PackagedProductRepository
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

class PackagedProductRepositoryImpl(private val api: CaloriesApi) : PackagedProductRepository {

    private val barcodeRegex = Regex("^\\d{8,14}$")

    override fun isBarcode(input: String): Boolean = barcodeRegex.matches(input.trim())

    override suspend fun search(input: String): Result<List<PackagedProduct>> {
        val query = input.trim()
        if (query.length < 2) return Result.success(emptyList())
        return try {
            if (isBarcode(query)) {
                val response = api.getProductByBarcode(query)
                if (!response.status.isSuccess()) return errorResult(response)
                Result.success(listOfNotNull(ProductMapper.toDomain(response.body<ProductDto>())))
            } else {
                val response = api.searchProducts(query)
                if (!response.status.isSuccess()) return errorResult(response)
                Result.success(response.body<ProductSearchResponseDto>().items.orEmpty().mapNotNull(ProductMapper::toDomain))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet kamu, lalu coba lagi."))
        }
    }

    /** A missing product is an empty result; otherwise the backend's message is shown when present. */
    private suspend fun errorResult(response: HttpResponse): Result<List<PackagedProduct>> {
        val error = runCatching { response.body<ApiErrorEnvelopeDto>().error }.getOrNull()
        if (response.status == HttpStatusCode.NotFound || error?.code == ERROR_NOT_FOUND) {
            return Result.success(emptyList())
        }
        val message = error?.message?.takeIf { it.isNotBlank() } ?: "Pencarian produk belum berhasil. Silakan coba lagi."
        return Result.failure(Exception(message))
    }

    private companion object {
        const val ERROR_NOT_FOUND = "product_not_found"
    }
}