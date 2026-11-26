package com.nafis.calories.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private fun currentStatus(): AVAuthorizationStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    var status by remember { mutableLongStateOf(currentStatus()) }

    LifecycleResumeEffect(Unit) {
        status = currentStatus()
        onPauseOrDispose { }
    }

    val denied = status == AVAuthorizationStatusDenied || status == AVAuthorizationStatusRestricted
    return object : CameraPermissionState {
        override val isGranted = status == AVAuthorizationStatusAuthorized
        override val isPermanentlyDenied = denied

        override fun request() {
            when {
                denied -> NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
                    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
                }
                status == AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                    dispatch_async(dispatch_get_main_queue()) { status = currentStatus() }
                }
            }
        }
    }
}