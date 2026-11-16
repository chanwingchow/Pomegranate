package com.chow.pomegranate.tool.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chow.pomegranate.Res
import com.chow.pomegranate.cancel
import com.chow.pomegranate.qr_clipboard_no_image
import com.chow.pomegranate.qr_no_code_found
import com.chow.pomegranate.qr_paste_image
import com.chow.pomegranate.qr_pick_image
import com.chow.pomegranate.qr_scan
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
actual fun QrScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 识别失败时的提示
    var message by remember { mutableStateOf<String?>(null) }

    val noCodeMessage = stringResource(Res.string.qr_no_code_found)
    val noImageMessage = stringResource(Res.string.qr_clipboard_no_image)

    /** 解码并回调。 */
    fun decode(image: BufferedImage?) {
        if (image == null) {
            message = noCodeMessage
            return
        }

        scope.launch {
            // 解码比较耗时，放到后台线程
            val text = withContext(Dispatchers.Default) { decodeQrCode(image) }

            if (text != null) onResult(text) else message = noCodeMessage
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.qr_scan),
                    style = MaterialTheme.typography.headlineSmall,
                )

                // 选择图片文件
                TextButton(
                    onClick = {
                        message = null
                        decode(readImageFile())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.qr_pick_image))
                }

                // 粘贴剪贴板里的图片
                TextButton(
                    onClick = {
                        message = null
                        val image = readClipboardImage()

                        if (image == null) message = noImageMessage else decode(image)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.qr_paste_image))
                }

                // 提示
                message?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

/** 弹出系统文件选择框读取图片。 */
private fun readImageFile(): BufferedImage? {
    val dialog = FileDialog(null as Frame?, "选择二维码图片", FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> name.isImageFile() }
        isVisible = true
    }

    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null

    return runCatching { ImageIO.read(File(directory, file)) }.getOrNull()
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
