package com.shoetrack.presentation.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
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
actual fun rememberPhotoPicker(
    onPhotoPicked: (ByteArray) -> Unit,
    @Suppress("UNUSED_PARAMETER") maxSidePx: Int,
): PhotoPicker {
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
        image?.uprightCopy()?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }?.let { onPhotoPicked(it.toByteArray()) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

// The camera stores photos sideways plus a rotation flag. Other platforms ignore that flag, so the
// rotation is baked into the pixels before the photo is saved and synced.
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.uprightCopy(): UIImage {
    if (imageOrientation == UIImageOrientation.UIImageOrientationUp) return this
    UIGraphicsBeginImageContextWithOptions(size, false, scale)
    drawInRect(size.useContents { CGRectMake(0.0, 0.0, width, height) })
    val redrawn = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return redrawn ?: this
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), bytes, length) }
}
