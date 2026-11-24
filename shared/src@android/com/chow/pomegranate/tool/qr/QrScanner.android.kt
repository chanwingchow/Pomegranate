package com.chow.pomegranate.tool.qr

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chow.pomegranate.Res
import com.chow.pomegranate.qr_image_read_failed
import com.chow.pomegranate.qr_need_camera
import com.chow.pomegranate.qr_no_code_found
import com.chow.pomegranate.qr_pick_image
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.Executors

/** 相机扫码界面，也支持从相册选图识别。 */
@Composable
actual fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 相机权限
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    // 没有权限时主动申请一次
    LaunchedEffect(granted) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    val snackbar = LocalSnackbarHostState.current
    val noCodeMessage = stringResource(Res.string.qr_no_code_found)
    val readFailedMessage = stringResource(Res.string.qr_image_read_failed)
    var selectingImage by remember { mutableStateOf(false) }

    // 从相册选一张图
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            selectingImage = false
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) { context.decodeQrImage(uri) }
            selectingImage = false
            result.fold(
                onSuccess = { value ->
                    if (value != null) onResult(value) else snackbar.showSnackbar(noCodeMessage)
                },
                onFailure = { snackbar.showSnackbar(readFailedMessage) },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (granted && !selectingImage) {
            CameraPreview(onResult = onResult)
        } else if (!granted) {
            // 没有相机权限时仍可从相册选图
            Text(
                text = stringResource(Res.string.qr_need_camera),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        // 关闭，避开状态栏
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.End,
                    ),
                )
                .padding(16.dp),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
            )
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
                .padding(24.dp),
            enabled = !selectingImage,
            onClick = {
                selectingImage = true
                pickLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
            Text(
                text = stringResource(Res.string.qr_pick_image),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CameraPreview(onResult: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember(context) { PreviewView(context) }
    val currentOnResult by rememberUpdatedState(onResult)

    DisposableEffect(previewView, lifecycleOwner) {
        val mainExecutor = context.mainExecutor
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var active = true
        var cameraProvider: ProcessCameraProvider? = null

        val fullHdResolution = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
            )
            .build()
        val preview = Preview.Builder()
            .setResolutionSelector(fullHdResolution)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(fullHdResolution)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisExecutor, QrAnalyzer { value ->
            mainExecutor.execute { if (active) currentOnResult(value) }
        })

        fun bindCamera() {
            val provider = cameraProvider ?: return
            val rotation = previewView.display?.rotation ?: return
            val viewPort = previewView.getViewPort(rotation) ?: return
            runCatching {
                provider.unbind(preview, analysis)
                preview.targetRotation = rotation
                analysis.targetRotation = rotation
                val useCases = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(analysis)
                    .build()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCases)
                if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    val previewInfo = preview.resolutionInfo
                    val analysisInfo = analysis.resolutionInfo
                    Log.d(
                        "QrScanner",
                        "Preview ${previewInfo?.resolution}, crop=${previewInfo?.cropRect}; " +
                            "analysis ${analysisInfo?.resolution}, crop=${analysisInfo?.cropRect}",
                    )
                }
            }.onFailure { Log.w("QrScanner", "Unable to bind camera preview and analysis", it) }
        }

        val layoutListener = View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) bindCamera()
        }
        previewView.addOnLayoutChangeListener(layoutListener)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                if (active) {
                    runCatching {
                        cameraProvider = providerFuture.get()
                        bindCamera()
                    }.onFailure { Log.w("QrScanner", "Unable to obtain camera provider", it) }
                }
            },
            mainExecutor,
        )

        // 弹窗关闭后，导航页面的 lifecycleOwner 仍可能处于 RESUMED。
        onDispose {
            active = false
            previewView.removeOnLayoutChangeListener(layoutListener)
            analysis.clearAnalyzer()
            cameraProvider?.unbind(preview, analysis)
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private var luminance = ByteArray(0)
    private var lastValue: String? = null

    override fun analyze(proxy: ImageProxy) {
        try {
            val crop = proxy.cropRect
            val width = crop.width()
            val height = crop.height()
            if (luminance.size != width * height) luminance = ByteArray(width * height)

            // Y 平面每像素占一个字节，逐行读取即可跳过行尾填充。
            val plane = proxy.planes[0]
            val buffer = plane.buffer.duplicate()
            val start = buffer.position() + crop.top * plane.rowStride + crop.left
            repeat(height) { row ->
                buffer.position(start + row * plane.rowStride)
                buffer.get(luminance, row * width, width)
            }

            val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
            val value = reader.decodeQr(source) ?: return
            if (value == lastValue) return
            lastValue = value
            onResult(value)
        } finally {
            proxy.close()
        }
    }
}

/** 是否已获得相机权限。 */
private fun Context.hasCameraPermission(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.decodeQrImage(uri: Uri): Result<String?> = runCatching {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val size = info.size
        val longestEdge = maxOf(size.width, size.height)
        if (longestEdge > MAX_GALLERY_IMAGE_EDGE) {
            val scale = MAX_GALLERY_IMAGE_EDGE.toDouble() / longestEdge
            decoder.setTargetSize(
                (size.width * scale).toInt().coerceAtLeast(1),
                (size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }
    try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        QRCodeReader().decodeQr(RGBLuminanceSource(width, height, pixels))
    } finally {
        bitmap.recycle()
    }
}

private fun QRCodeReader.decodeQr(source: LuminanceSource): String? {
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        decode(bitmap).text
    } catch (_: ReaderException) {
        try {
            // 复用二值化结果，尝试识别白码黑底的二维码。
            bitmap.blackMatrix.flip()
            decode(bitmap).text
        } catch (_: ReaderException) {
            null
        }
    }
}

private const val MAX_GALLERY_IMAGE_EDGE = 2560
