package com.nafis.calories.ui.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import co.touchlab.kermit.Logger
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.nafis.calories.utils.CameraUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private val log = Logger.withTag("Camera")

private class AndroidCameraController(private val context: Context) : CameraController {

    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setTargetRotation(Surface.ROTATION_0)
        .build()

    override suspend fun capture(): ByteArray? {
        val image = suspendCancellableCoroutine<ImageProxy?> { continuation ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        continuation.resume(image)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        log.w(exception) { "Capture failed" }
                        continuation.resume(null)
                    }
                }
            )
        } ?: return null

        return withContext(Dispatchers.Default) {
            try {
                val bitmap = CameraUtils.imageProxyToBitmap(image)
                JpegEncoder.encode(CameraUtils.rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat()))
            } catch (e: Exception) {
                log.w(e) { "Captured frame could not be encoded" }
                null
            } finally {
                image.close()
            }
        }
    }
}

@Composable
actual fun rememberCameraController(): CameraController {
    val context = LocalContext.current
    return remember(context) { AndroidCameraController(context) }
}

@Composable
actual fun CameraPreview(controller: CameraController, modifier: Modifier) {
    val imageCapture = (controller as AndroidCameraController).imageCapture
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).also { previewView ->
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    } catch (e: Exception) {
                        log.w(e) { "Camera could not be bound" }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
    )
}

@Composable
actual fun rememberGalleryPicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val jpeg = withContext(Dispatchers.IO) { readAsJpeg(context, uri) }
            jpeg?.let(onImagePicked)
        }
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

private fun readAsJpeg(context: Context, uri: Uri): ByteArray? = try {
    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(JpegEncoder::encode)
} catch (e: Exception) {
    log.w(e) { "Picked image could not be read" }
    null
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    val granted = permission.status.isGranted
    val permanentlyDenied = hasRequested && !granted && !permission.status.shouldShowRationale

    return object : CameraPermissionState {
        override val isGranted = granted
        override val isPermanentlyDenied = permanentlyDenied

        override fun request() {
            if (permanentlyDenied) {
                openAppSettings(context)
            } else {
                hasRequested = true
                permission.launchPermissionRequest()
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}