package com.nafis.calories.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageStorageUtil {

    private const val HISTORY_IMAGES_DIR = "history_images"
    private const val IMAGE_QUALITY = 90 // JPEG quality (0-100)

    fun saveImage(context: Context, bitmap: Bitmap): String? {
        return try {
            // Step 1: Create directory
            val directory = File(context.filesDir, HISTORY_IMAGES_DIR)
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) {
                    return null // Failed to create directory
                }
            }

            // Step 2: Generate filename (timestamp-based for uniqueness)
            val timestamp = System.currentTimeMillis()
            val filename = "$timestamp.jpg"
            val file = File(directory, filename)

            // Step 3: Compress & write to file
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
                outputStream.flush()
            }

            // Step 4: Return relative path (for database storage)
            "$HISTORY_IMAGES_DIR/$filename"
        } catch (e: IOException) {
            e.printStackTrace()
            null // Return null on failure (graceful degradation)
        }
    }

    fun loadImage(context: Context, relativePath: String): Bitmap? {
        return try {
            // Step 1: Construct full path
            val file = File(context.filesDir, relativePath)

            // Step 2: Check existence
            if (!file.exists()) {
                return null
            }

            // Step 3: Decode to Bitmap
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImage(context: Context, relativePath: String): Boolean {
        return try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                false // File not exists
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAllImages(context: Context): Int {
        return try {
            val directory = File(context.filesDir, HISTORY_IMAGES_DIR)
            if (!directory.exists()) {
                return 0
            }

            var deletedCount = 0
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) {
                    deletedCount++
                }
            }
            deletedCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun getStorageSize(context: Context): Long {
        return try {
            val directory = File(context.filesDir, HISTORY_IMAGES_DIR)
            if (!directory.exists()) {
                return 0L
            }

            var totalSize = 0L
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
            totalSize
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}