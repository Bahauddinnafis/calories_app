package com.nafis.calories.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Decodes JPEG/PNG bytes once per instance; null when absent or unreadable. */
@Composable
fun rememberImageBitmap(bytes: ByteArray?): ImageBitmap? = remember(bytes) {
    bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
}