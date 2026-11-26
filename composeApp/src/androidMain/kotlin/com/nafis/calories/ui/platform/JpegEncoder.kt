package com.nafis.calories.ui.platform

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

internal object JpegEncoder {

    private const val MAX_DIMENSION = 1536
    private const val QUALITY = 85

    fun encode(bitmap: Bitmap): ByteArray {
        val scaled = scaleDown(bitmap)
        return ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)
            output.toByteArray()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (width > height) {
            MAX_DIMENSION to (MAX_DIMENSION / ratio).toInt()
        } else {
            (MAX_DIMENSION * ratio).toInt() to MAX_DIMENSION
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
    }
}