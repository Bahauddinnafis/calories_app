package com.nafis.calories.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private const val IMAGE_TYPE = "public.image"

private class PickerDelegate(
    private val scope: CoroutineScope,
    private val onImagePicked: () -> ((ByteArray) -> Unit)
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return
        provider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE) { data, _ ->
            data ?: return@loadDataRepresentationForTypeIdentifier
            scope.launch {
                val jpeg = withContext(Dispatchers.Default) { UIImage(data = data).toUploadJpeg() }
                jpeg?.let(onImagePicked())
            }
        }
    }
}

@Composable
actual fun rememberGalleryPicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val presenter: UIViewController = LocalUIViewController.current
    val scope = rememberCoroutineScope()
    val currentOnImagePicked = rememberUpdatedState(onImagePicked)
    val delegate = remember { PickerDelegate(scope) { currentOnImagePicked.value } }

    return remember(presenter, delegate) {
        {
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = configuration).apply { this.delegate = delegate }
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}