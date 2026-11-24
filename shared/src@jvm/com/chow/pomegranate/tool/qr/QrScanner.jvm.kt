package com.chow.pomegranate.tool.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.cancel
import com.chow.pomegranate.qr_clipboard_no_image
import com.chow.pomegranate.qr_image_read_failed
import com.chow.pomegranate.qr_no_code_found
import com.chow.pomegranate.qr_paste_image
import com.chow.pomegranate.qr_pick_image
import com.chow.pomegranate.qr_scan
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.awt.Image
import java.awt.datatransfer.DataFlavor

/**
 * 桌面端扫码界面。
 *
 * 桌面端没有可靠的相机能力，改成从图片识别：选图片文件或粘贴剪贴板里的图片。
 */
@Composable
actual fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val noCodeMessage = stringResource(Res.string.qr_no_code_found)
    val noImageMessage = stringResource(Res.string.qr_clipboard_no_image)
    val readFailedMessage = stringResource(Res.string.qr_image_read_failed)

    suspend fun decode(image: BufferedImage) {
        val value = withContext(Dispatchers.Default) { decodeQrCode(image) }
        if (value != null) onResult(value) else snackbar.showSnackbar(noCodeMessage)
    }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.qr_scan), style = MaterialTheme.typography.headlineSmall)
                TextButton(
                    onClick = {
                        val file = chooseImageFile()
                        if (file != null) scope.launch {
                            val image = withContext(Dispatchers.IO) { runCatching { ImageIO.read(file) }.getOrNull() }
                            if (image == null) snackbar.showSnackbar(readFailedMessage) else decode(image)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.qr_pick_image))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            val image = readClipboardImage()
                            if (image == null) snackbar.showSnackbar(noImageMessage) else decode(image)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.qr_paste_image))
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

private fun chooseImageFile(): File? {
    val dialog = FileDialog(null as Frame?, "选择二维码图片", FileDialog.LOAD)
    return try {
        dialog.setFilenameFilter { _, name -> name.isImageFile() }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        File(directory, file)
    } finally {
        dialog.dispose()
    }
}

/** 读取剪贴板里的图片。 */
private fun readClipboardImage(): BufferedImage? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    // 剪贴板里没有图片时直接返回
    if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) return null

    val image = runCatching {
        clipboard.getData(DataFlavor.imageFlavor) as? Image
    }.getOrNull() ?: return null

    val width = image.getWidth(null)
    val height = image.getHeight(null)

    if (width <= 0 || height <= 0) return null

    // 转成可读像素的 BufferedImage
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { buffered ->
        buffered.createGraphics().apply {
            drawImage(image, 0, 0, null)
            dispose()
        }
    }
}

/** 用 ZXing 解码二维码，识别不到时返回 `null`。 */
private fun decodeQrCode(image: BufferedImage): String? {
    val width = image.width
    val height = image.height
    val pixels = IntArray(width * height)

    image.getRGB(0, 0, width, height, pixels, 0, width)

    val source = RGBLuminanceSource(width, height, pixels)
    val bitmap = BinaryBitmap(HybridBinarizer(source))

    val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    return runCatching { reader.decode(bitmap).text }.getOrNull()
}

/** 常见图片后缀。 */
private fun String.isImageFile(): Boolean =
    substringAfterLast('.', missingDelimiterValue = "").lowercase() in IMAGE_EXTENSIONS

/** 支持的图片后缀。 */
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "bmp", "gif", "webp")
