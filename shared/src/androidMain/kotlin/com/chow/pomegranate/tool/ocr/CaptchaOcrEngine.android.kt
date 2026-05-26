package com.chow.pomegranate.tool.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.googlecode.tesseract.android.TessBaseAPI
import com.moondeveloper.ocr.OcrEngine
import com.moondeveloper.ocr.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemPathSeparator

actual fun createCaptchaOcrEngine(
    block: CaptchaOcrEngineConfig.() -> Unit,
): OcrEngine {
    val config = CaptchaOcrEngineConfig().apply(block)

    return TesseractOcrEngineImpl(config)
}

private class TesseractOcrEngineImpl(
    private val config: CaptchaOcrEngineConfig,
) : OcrEngine {
    override suspend fun recognize(image: ByteArray): OcrResult = withContext(Dispatchers.IO) {
        TessBaseAPI().use {
            // tessdata 的上一级目录
            val dir = config.dataPath
                .removeSuffix("$SystemPathSeparator")
                .removeSuffix("${SystemPathSeparator}tessdata")

            // 初始化 Tesseract
            require(init(dir, config.language))

            // 创建 Bitmap
            val bitmap = BitmapFactory.decodeByteArray(
                image,
                0,
                image.size,
                BitmapFactory.Options().apply { inMutable = true },
            )

            try {
                // 灰度化处理
                bitmap.greyscale(config.borderWidth)

                // 单行识别
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
                // 白名单
                setVariable(
                    TessBaseAPI.VAR_CHAR_WHITELIST,
                    config.charWhiteList,
                )

                // 识别
                setImage(bitmap)

                OcrResult(
                    fullText = utF8Text,
                    blocks = emptyList(),
                )
            } finally {
                bitmap.recycle()
            }
        }
    }

    override fun isAvailable() = true
}

/**
 * 使用 [TessBaseAPI]，最后自动释放资源。
 */
private inline fun <R> TessBaseAPI.use(block: TessBaseAPI.() -> R): R {
    return try {
        block()
    } finally {
        recycle()
    }
}

/**
 * [Bitmap] 灰度化处理。
 */
private fun Bitmap.greyscale(borderWidth: Int) {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    for (index in pixels.indices) {
        val x = index % width
        val y = index / width
        val isBorder = x < borderWidth || x >= width - borderWidth ||
                y < borderWidth || y >= height - borderWidth

        pixels[index] = when {
            // 去除边框
            isBorder -> Color.WHITE
            // 灰度化
            Color.luminance(pixels[index]) < 0.443f -> Color.BLACK
            else -> Color.WHITE
        }
    }

    setPixels(pixels, 0, width, 0, 0, width, height)
}