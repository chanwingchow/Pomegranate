package com.chow.pomegranate.tool.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chow.pomegranate.Res
import com.chow.pomegranate.qr_need_camera
import com.chow.pomegranate.qr_no_code_found
import com.chow.pomegranate.qr_pick_image
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

/** 相机扫码界面，也支持从相册选图识别。 */
@Composable
actual fun QrScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
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

    // 只识别二维码；模型内置，不需要联网下载
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    // 相册图片识别失败时的提示
    var message by remember { mutableStateOf<String?>(null) }
    val noCodeMessage = stringResource(Res.string.qr_no_code_found)

    DisposableEffect(Unit) {
        onDispose { scanner.close() }
    }

    // 从相册选一张图
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            // 读图片是 IO 操作，放到后台线程
            val image = withContext(Dispatchers.IO) { context.loadImage(uri) }

            if (image == null) {
                message = noCodeMessage
                return@launch
            }

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstNotNullOfOrNull { it.rawValue }

                    if (value != null) onResult(value) else message = noCodeMessage
                }
                .addOnFailureListener { message = noCodeMessage }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // 不内缩，取景框才能铺满到状态栏和导航栏
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (granted) {
                CameraPreview(scanner = scanner, onResult = onResult)
            } else {
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
                onClick = onDismiss,
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                        ),
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 识别失败提示
                message?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // 从相册选择
                Button(
                    onClick = {
                        message = null
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
    }
}

/**
 * 相机预览与二维码识别。
 *
 * @param scanner 与相册识别共用的识别器
 */
@Composable
private fun CameraPreview(
    scanner: BarcodeScanner,
    onResult: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // 已回调过就不再处理，避免连续识别重复触发
    val handled = remember { AtomicBoolean(false) }

    // 关掉扫码界面时必须显式解绑相机。
    // 这里不能只依赖 lifecycleOwner：它在 NavDisplay 里是当前场景的 owner，
    // 只要宿主弹窗还在栈上就一直是 RESUMED，相机不会被自动释放——
    // 表现为隐私指示灯常亮、耗电，其他 App 打不开相机。
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ctx.mainExecutor

            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val analysis = ImageAnalysis.Builder()
                        // 只保留最新一帧，扫码不需要排队
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, QrAnalyzer(scanner, handled, onResult)) }

                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                },
                executor,
            )

            previewView
        },
    )
}

/**
 * 相机帧分析器。
 *
 * 将 [ImageProxy] 的底层图像交给 ML Kit，并确保每帧最终都会关闭。
 */
private class QrAnalyzer(
    private val scanner: BarcodeScanner,
    private val handled: AtomicBoolean,
    private val onResult: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(proxy: ImageProxy) {
        val mediaImage = proxy.image

        if (mediaImage == null || handled.get()) {
            proxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }

                // 只回调第一次识别结果
                if (value != null && handled.compareAndSet(false, true)) {
                    onResult(value)
                }
            }
            // 无论成功失败都要关掉这一帧，否则后续帧不会送进来
            .addOnCompleteListener { proxy.close() }
    }
}

/** 是否已获得相机权限。 */
private fun Context.hasCameraPermission(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** 读取相册图片，失败时返回 `null`。 */
private fun Context.loadImage(uri: Uri): InputImage? =
    runCatching { InputImage.fromFilePath(this, uri) }.getOrNull()
