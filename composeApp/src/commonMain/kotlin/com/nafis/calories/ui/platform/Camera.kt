package com.nafis.calories.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

interface CameraController {
    /** JPEG of the current frame, scaled for upload; null when the capture failed. */
    suspend fun capture(): ByteArray?
}

@Stable
interface CameraPermissionState {
    val isGranted: Boolean
    val isPermanentlyDenied: Boolean

    fun request()
}

@Composable
expect fun rememberCameraController(): CameraController

@Composable
expect fun CameraPreview(controller: CameraController, modifier: Modifier = Modifier)

/** Returns a launcher; the picked image arrives as an upload-ready JPEG. */
@Composable
expect fun rememberGalleryPicker(onImagePicked: (ByteArray) -> Unit): () -> Unit

@Composable
expect fun rememberCameraPermissionState(): CameraPermissionState