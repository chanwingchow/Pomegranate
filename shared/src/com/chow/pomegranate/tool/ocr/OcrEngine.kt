package com.chow.pomegranate.tool.ocr

/** 验证码识别限定字符集。 */
internal const val OCR_CHAR_WHITE_LIST = "123abcdmnvxz"

/** 验证码边框宽度，识别前会被抹白。 */
internal const val OCR_BORDER_WIDTH = 3

/** 训练数据在资源中的路径。 */
internal const val TRAINED_DATA_RESOURCE_PATH = "/tessdata/eng.traineddata"

/** 训练数据文件名。 */
internal const val TRAINED_DATA_NAME = "eng.traineddata"

/** 训练数据目录名。 */
internal const val TESSDATA_DIRECTORY = "tessdata"

/**
 * 验证码识别引擎。
 *
 * 平台实现：
 * - Android：Tesseract4Android；
 * - JVM：Tess4J；
 * - iOS：Vision 框架。
 */
interface OcrEngine {
    /**
     * 识别 [image] 中的文字。
     *
     * @param image 图片字节，支持 PNG、JPEG
     */
    suspend fun recognize(image: ByteArray): String

    /** 当前平台是否支持识别。 */
    fun isAvailable(): Boolean
}

/**
 * 返回当前平台的验证码识别引擎。
 *
 * 训练数据随应用打包，首次使用时写入本机目录，完全离线可用。
 */
expect fun createCaptchaOcrEngine(): OcrEngine
