package com.chow.pomegranate.tool.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

actual fun createCaptchaOcrEngine(): OcrEngine = JvmCaptchaOcrEngine()

/**
 * 基于 Tess4J 的验证码识别引擎。
 *
 * 训练数据从 classpath 中取出并写入本机目录，不需要联网下载。
 */
private class JvmCaptchaOcrEngine : OcrEngine {
    /** tessdata 目录。 */
    private val tessdataDirectory: File by lazy { prepareTessdata() }

    override suspend fun recognize(image: ByteArray): String = withContext(Dispatchers.IO) {
        with(Tesseract()) {
            setDatapath(tessdataDirectory.absolutePath)
            setLanguage("eng")

            ByteArrayInputStream(image).use { stream ->
                val bufferedImage = ImageIO.read(stream) ?: error("无法解码验证码图片。")

                // 灰度化处理
                bufferedImage.greyscale()

                // 验证码为单行文本
                setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_LINE)
                // 限定识别字符集
                setVariable("tessedit_char_whitelist", OCR_CHAR_WHITE_LIST)

                doOCR(bufferedImage).orEmpty()
            }
        }
    }

    override fun isAvailable() = runCatching {
        File(tessdataDirectory, TRAINED_DATA_NAME).exists()
    }.getOrDefault(false)
}

/**
 * 将 classpath 中的训练数据写入本机目录，并返回 tessdata 目录。
 *
 * 已存在且大小一致时直接复用。
 */
private fun prepareTessdata(): File {
    val directory = File(
        File(System.getProperty("user.home"), ".pomegranate"),
        TESSDATA_DIRECTORY,
    ).apply { mkdirs() }

    val file = File(directory, TRAINED_DATA_NAME)

    val trainedData = JvmCaptchaOcrEngine::class.java
        .getResourceAsStream(TRAINED_DATA_RESOURCE_PATH)
        ?.readBytes()
        ?: return directory

    if (!file.exists() || file.length() != trainedData.size.toLong()) {
        file.writeBytes(trainedData)
    }

    return directory
}

/**
 * 灰度化并去除边框。
 */
private fun BufferedImage.greyscale() {
    val pixels = IntArray(width * height)
    getRGB(0, 0, width, height, pixels, 0, width)

    for (index in pixels.indices) {
        val x = index % width
        val y = index / width
        val isBorder = x < OCR_BORDER_WIDTH || x >= width - OCR_BORDER_WIDTH ||
                y < OCR_BORDER_WIDTH || y >= height - OCR_BORDER_WIDTH

        pixels[index] = when {
            // 去除边框
            isBorder -> Color.WHITE.rgb
            // 按亮度二值化
            else -> {
                val pixel = pixels[index]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                if ((red + green + blue) / 3 < 113) Color.BLACK.rgb else Color.WHITE.rgb
            }
        }
    }

    setRGB(0, 0, width, height, pixels, 0, width)
}
