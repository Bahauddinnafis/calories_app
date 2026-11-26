package com.nafis.calories.ui.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.fileDataRepresentation
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import kotlin.coroutines.resume

private val log = Logger.withTag("Camera")

private class IosCameraController : CameraController {

    val session = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private var pendingDelegate: PhotoCaptureDelegate? = null
    private val isConfigured: Boolean = configure()

    @OptIn(ExperimentalForeignApi::class)
    private fun configure(): Boolean {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return false
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return false
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetPhoto
        if (session.canAddInput(input)) session.addInput(input)
        if (session.canAddOutput(photoOutput)) session.addOutput(photoOutput)
        session.commitConfiguration()
        return true
    }

    fun start() {
        if (!isConfigured) return
        onSessionQueue { if (!session.running) session.startRunning() }
    }

    fun stop() {
        onSessionQueue { if (session.running) session.stopRunning() }
    }

    override suspend fun capture(): ByteArray? {
        if (!session.running) return null
        val data = suspendCancellableCoroutine<NSData?> { continuation ->
            // AVCapturePhotoOutput keeps only a weak reference to its delegate.
            val delegate = PhotoCaptureDelegate { data ->
                pendingDelegate = null
                continuation.resume(data)
            }
            pendingDelegate = delegate
            photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate)
        } ?: return null
        return withContext(Dispatchers.Default) { UIImage(data = data).toUploadJpeg() }
    }

    private fun onSessionQueue(block: () -> Unit) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u), block)
    }
}

private class PhotoCaptureDelegate(
    private val onResult: (NSData?) -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    @ObjCSignatureOverride
    override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
        if (error != null) log.w { "Capture failed: ${error.localizedDescription}" }
        onResult(if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectZero.readValue()) {

    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

@Composable
actual fun rememberCameraController(): CameraController = remember { IosCameraController() }

@Composable
actual fun CameraPreview(controller: CameraController, modifier: Modifier) {
    val camera = controller as IosCameraController
    DisposableEffect(camera) {
        camera.start()
        onDispose { camera.stop() }
    }
    UIKitView(
        factory = { CameraPreviewView(camera.session) },
        modifier = modifier
    )
}