package com.nafis.calories.ui.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min

private const val MAX_DIMENSION = 1536.0
private const val QUALITY = 0.85

/** Same upload format as Android: longest side at most 1536 px, JPEG quality 85, orientation applied. */
@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.toUploadJpeg(): ByteArray? {
    val (width, height) = size.useContents { width * scale to height * scale }
    if (width <= 0.0 || height <= 0.0) return null
    val ratio = min(1.0, MAX_DIMENSION / max(width, height))
    val targetWidth = width * ratio
    val targetHeight = height * ratio

    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
    val scaled = renderer.imageWithActions { _ ->
        drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    }
    return UIImageJPEGRepresentation(scaled, QUALITY)?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
    return result
}