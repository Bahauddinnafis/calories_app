package com.nafis.calories.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object CameraUtils {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        if (image.planes.isEmpty()) {
            throw IllegalStateException("Image has no planes")
        }

        return when (image.format) {
            ImageFormat.JPEG -> {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Failed to decode JPEG image")
            }
            ImageFormat.YUV_420_888, ImageFormat.NV21 -> {
                if (image.planes.size < 3) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: throw IllegalStateException("Failed to decode YUV single-plane image")
                } else {
                    val yBuffer = image.planes[0].buffer
                    val uBuffer = image.planes[1].buffer
                    val vBuffer = image.planes[2].buffer

                    val ySize = yBuffer.remaining()
                    val uSize = uBuffer.remaining()
                    val vSize = vBuffer.remaining()

                    val nv21 = ByteArray(ySize + uSize + vSize)
                    yBuffer.get(nv21, 0, ySize)
                    vBuffer.get(nv21, ySize, vSize)
                    uBuffer.get(nv21, ySize + vSize, uSize)

                    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                    val out = ByteArrayOutputStream()
                    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
                    val imageBytes = out.toByteArray()

                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ?: throw IllegalStateException("Failed to decode YUV image")
                }
            }
            else -> {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Failed to decode image")
            }
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}