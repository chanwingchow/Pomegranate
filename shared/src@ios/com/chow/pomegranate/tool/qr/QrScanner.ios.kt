package com.chow.pomegranate.tool.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.chow.pomegranate.Res
import com.chow.pomegranate.qr_need_camera
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.stringResource
import platform.AVFoundation.AVAuthorizationStatusAuthorized
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
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import com.chow.pomegranate.qr_image_read_failed
import com.chow.pomegranate.qr_no_code_found
import com.chow.pomegranate.qr_pick_image
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerControllerSourceType

/** 相机扫码界面。 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    // 相机权限
    var granted by remember { mutableStateOf(hasCameraPermission()) }

    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val noCodeMessage = stringResource(Res.string.qr_no_code_found)
    val readFailedMessage = stringResource(Res.string.qr_image_read_failed)
    val imagePicked by rememberUpdatedState<(UIImage?) -> Unit> { image ->
        val value = image?.let(::decodeQrFromImage)
        if (value != null) onResult(value)
        else scope.launch { snackbar.showSnackbar(if (image == null) readFailedMessage else noCodeMessage) }
    }

    LaunchedEffect(granted) {
        if (!granted) granted = requestCameraPermission()
    }

    val pickerDelegate = remember { ImagePickerDelegate { imagePicked(it) } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraPreview(onResult = onResult)
        } else {
            Text(
                text = stringResource(Res.string.qr_need_camera),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(16.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
        }
        Button(
            onClick = { presentImagePicker(pickerDelegate) },
            modifier = Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(24.dp),
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
            Text(stringResource(Res.string.qr_pick_image), Modifier.padding(start = 8.dp))
        }
    }
}

/** 相机预览与二维码识别。 */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun CameraPreview(
    onResult: (String) -> Unit,
) {
    val currentOnResult by rememberUpdatedState(onResult)
    val delegate = remember {
        var lastValue: String? = null
        QrMetadataDelegate { value ->
            if (value != lastValue) {
                lastValue = value
                currentOnResult(value)
            }
        }
    }

    val session = remember { createSession(delegate) }

    DisposableEffect(session) {
        session?.startRunning()

        onDispose { session?.stopRunning() }
    }

    if (session == null) {
        // 相机不可用时给出提示
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.qr_need_camera),
                modifier = Modifier.padding(32.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = { CameraPreviewView(session) },
    )
}

/**
 * 预览视图。
 *
 * 在 [UIView.layoutSubviews] 里同步预览层尺寸，避免旋转或尺寸变化时画面不同步。
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(
    session: AVCaptureSession,
) : UIView(frame = CGRectZero.readValue()) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        // 关掉隐式动画，否则尺寸变化时画面会漂移
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }
}

/** 元数据输出代理，识别到二维码时回调。 */
private class QrMetadataDelegate(
    private val onResult: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val value = didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstOrNull()
            ?.stringValue
            ?: return

        onResult(value)
    }
}

/**
 * 创建并配置相机会话。
 *
 * 任一环节不可用时返回 `null`，由界面给出提示。
 */
@OptIn(ExperimentalForeignApi::class)
private fun createSession(delegate: QrMetadataDelegate): AVCaptureSession? {
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return null
    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return null

    val session = AVCaptureSession()

    if (!session.canAddInput(input)) return null
    session.addInput(input)

    val output = AVCaptureMetadataOutput()

    if (!session.canAddOutput(output)) return null
    session.addOutput(output)

    // 只识别二维码
    output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
    output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

    return session
}

/** 是否已获得相机权限。 */
private fun hasCameraPermission(): Boolean =
    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
        AVAuthorizationStatusAuthorized

/** 申请相机权限。 */
private suspend fun requestCameraPermission(): Boolean = suspendCancellableCoroutine { cont ->
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        cont.resume(granted)
    }
}

/**
 * 相册选择代理。
 *
 * 选完或取消都会关闭选择器。
 */
private class ImagePickerDelegate(
    private val onPicked: (UIImage?) -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) { onPicked(image) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

/** 弹出系统图片选择器。 */
private fun presentImagePicker(delegate: ImagePickerDelegate) {
    val picker = UIImagePickerController().apply {
        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        this.delegate = delegate
    }

    UIApplication.sharedApplication.keyWindow
        ?.rootViewController
        ?.presentViewController(picker, animated = true, completion = null)
}

/**
 * 用 Vision 从图片里识别二维码。
 *
 * 与验证码 OCR 用的是同一套 Vision 能力，识别不到时返回 `null`。
 */
@OptIn(ExperimentalForeignApi::class)
private fun decodeQrFromImage(image: UIImage): String? {
    val data = UIImagePNGRepresentation(image) ?: return null

    val request = VNDetectBarcodesRequest { _, _ ->
        // 结果在下面统一读取
    }

    val handler = VNImageRequestHandler(data = data, options = emptyMap<Any?, Any>())

    runCatching { handler.performRequests(listOf(request), error = null) }

    return request.results.orEmpty()
        .filterIsInstance<VNBarcodeObservation>()
        .firstOrNull { it.symbology == VNBarcodeSymbologyQR }
        ?.payloadStringValue
}
