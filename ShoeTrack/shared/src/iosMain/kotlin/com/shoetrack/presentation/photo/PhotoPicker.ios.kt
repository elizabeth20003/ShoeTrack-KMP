package com.shoetrack.presentation.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

private const val JPEG_QUALITY = 0.8

@Composable
actual fun rememberPhotoPicker(onPhotoPicked: (ByteArray) -> Unit): PhotoPicker {
    val currentOnPhotoPicked = rememberUpdatedState(onPhotoPicked)
    return remember { IosPhotoPicker { currentOnPhotoPicked.value(it) } }
}

private class IosPhotoPicker(onPhotoPicked: (ByteArray) -> Unit) : PhotoPicker {

    // UIKit keeps only a weak reference to its delegate, so this class must hold it.
    private val delegate = PickerDelegate(onPhotoPicked)

    override fun takePhoto() = present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    override fun pickFromGallery() =
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)

    private fun present(sourceType: UIImagePickerControllerSourceType) {
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) return
        val picker = UIImagePickerController().apply {
            this.sourceType = sourceType
            this.delegate = this@IosPhotoPicker.delegate
        }
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

private class PickerDelegate(
    private val onPhotoPicked: (ByteArray) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        image?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }?.let { onPhotoPicked(it.toByteArray()) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), bytes, length) }
}
