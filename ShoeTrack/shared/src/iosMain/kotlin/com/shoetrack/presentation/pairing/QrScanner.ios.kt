package com.shoetrack.presentation.pairing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberQrScanner(onCode: (String) -> Unit): QrScanner? {
    val currentOnCode = rememberUpdatedState(onCode)
    return remember {
        object : QrScanner {
            override fun scan() {
                val scanner = ScannerViewController { currentOnCode.value(it) }
                topViewController()?.presentViewController(scanner, animated = true, completion = null)
            }
        }
    }
}

// A dialog or another sheet may already be on screen, and only the top one can present.
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

/** A sheet with the camera preview that closes itself as soon as it reads a QR code. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ScannerViewController(private val onCode: (String) -> Unit) :
    UIViewController(nibName = null, bundle = null),
    AVCaptureMetadataOutputObjectsDelegateProtocol {
    private val session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var finished = false

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.blackColor
        val camera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, error = null) ?: return
        val output = AVCaptureMetadataOutput()
        if (!session.canAddInput(input) || !session.canAddOutput(output)) return
        session.addInput(input)
        session.addOutput(output)
        output.setMetadataObjectsDelegate(this, dispatch_get_main_queue())
        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

        val layer = AVCaptureVideoPreviewLayer(session = session)
        layer.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(layer)
        previewLayer = layer
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        // startRunning blocks until the camera is up, so it stays off the main thread.
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.startRunning() }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        session.stopRunning()
    }

    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        val code = (didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.stringValue
        if (code == null || finished) return
        finished = true
        dismissViewControllerAnimated(true) { onCode(code) }
    }
}
