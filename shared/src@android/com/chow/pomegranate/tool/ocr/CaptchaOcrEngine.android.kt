package com.chow.pomegranate.tool.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File

actual fun createCaptchaOcrEngine(): OcrEngine = AndroidCaptchaOcrEngine()

/**
 * 基于 Tesseract4Android 的验证码识别引擎。
 *
 * 训练数据从 classpath 资源复制到缓存目录，不需要联网下载。
 */
private class AndroidCaptchaOcrEngine : OcrEngine {
    /** tessdata 目录。 */
    private val tessdataDirectory: File by lazy { prepareTessdata(context()) }

    override suspend fun recognize(image: ByteArray): String = withContext(Dispatchers.IO) {
        TessBaseAPI().use {
            // Tesseract 要求传入 tessdata 的上一级目录
            require(init(tessdataDirectory.parentFile?.absolutePath, "eng")) {
                "无法初始化验证码识别引擎。"
            }

            val bitmap = BitmapFactory.decodeByteArray(
                image,
                0,
                image.size,
                BitmapFactory.Options().apply { inMutable = true },
            ) ?: error("无法解码验证码图片。")

            try {
                // 灰度化处理
                bitmap.greyscale()

                // 验证码为单行文本
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
                // 限定识别字符集
                setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, OCR_CHAR_WHITE_LIST)

                setImage(bitmap)

                utF8Text.orEmpty()
            } finally {
                bitmap.recycle()
            }
        }
    }

    override fun isAvailable() = runCatching {
        File(tessdataDirectory, TRAINED_DATA_NAME).exists()
    }.getOrDefault(false)
}

/** 返回应用上下文。 */
private fun context(): Context = KoinPlatform.getKoin().get<Context>()

/**
 * 将 classpath 中的训练数据复制到缓存目录，并返回 tessdata 目录。
 *
 * 训练数据只放一份在 commonMain 资源里，Android 与桌面共用：
 * 之前 Android 另外在 assets 里放了一份，而 commonMain 那份同样会被打进 APK，
 * 导致 APK 里出现两份 4MB 的 eng.traineddata。已存在且大小一致时直接复用。
 */
private fun prepareTessdata(context: Context): File {
    val directory = File(context.cacheDir, TESSDATA_DIRECTORY).apply { mkdirs() }
    val file = File(directory, TRAINED_DATA_NAME)

    val resource = AndroidCaptchaOcrEngine::class.java
        .getResourceAsStream(TRAINED_DATA_RESOURCE_PATH)
        ?: return directory

    resource.use { input ->
        // 用流式复制而不是 InputStream.available()：后者对 classpath 资源并不保证返回完整长度
        val copied = File(directory, "$TRAINED_DATA_NAME.new")
        copied.outputStream().use { output -> input.copyTo(output) }

        if (file.exists() && file.length() == copied.length()) {
            copied.delete()
        } else {
            file.delete()
            if (!copied.renameTo(file)) {
                copied.copyTo(file, overwrite = true)
                copied.delete()
            }
        }
    }

    return directory
}

/**
 * 使用 [TessBaseAPI]，结束后自动释放资源。
 */
private inline fun <R> TessBaseAPI.use(block: TessBaseAPI.() -> R): R {
    return try {
        block()
    } finally {
        recycle()
    }
}

/**
 * 灰度化并去除边框。
 */
private fun Bitmap.greyscale() {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    for (index in pixels.indices) {
        val x = index % width
        val y = index / width
        val isBorder = x < OCR_BORDER_WIDTH || x >= width - OCR_BORDER_WIDTH ||
                y < OCR_BORDER_WIDTH || y >= height - OCR_BORDER_WIDTH

        pixels[index] = when {
            // 去除边框
            isBorder -> Color.WHITE
            // 按亮度二值化
            Color.luminance(pixels[index]) < 0.443f -> Color.BLACK
            else -> Color.WHITE
        }
    }

    setPixels(pixels, 0, width, 0, 0, width, height)
}
