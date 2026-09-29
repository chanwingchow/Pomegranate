package com.chow.pomegranate.tool.ocr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

actual fun createCaptchaOcrEngine(): OcrEngine = IosCaptchaOcrEngine()

/**
 * 基于系统 Vision 框架的验证码识别引擎。
 *
 * iOS 自带文字识别能力，不需要携带 Tesseract 训练数据。
 */
@OptIn(ExperimentalForeignApi::class)
private class IosCaptchaOcrEngine : OcrEngine {
    override suspend fun recognize(image: ByteArray): String {
        val request = VNRecognizeTextRequest { _, _ ->
            // 识别结果在下方统一读取
        }.apply {
            // 验证码为单行短文本，使用精确识别并关闭语言矫正
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            usesLanguageCorrection = false
        }

        val handler = VNImageRequestHandler(data = image.toNSData(), options = emptyMap<Any?, Any>())
        handler.performRequests(listOf(request), error = null)

        val observations = request.results.orEmpty()
            .filterIsInstance<VNRecognizedTextObservation>()

        return buildString {
            observations.forEach { observation ->
                // topCandidates 返回 List<*>，需要转换为 VNRecognizedText
                val candidate = observation.topCandidates(1u).firstOrNull() as? VNRecognizedText
                    ?: return@forEach

                append(candidate.string)
            }
        }
    }

    override fun isAvailable() = true
}

/** 将 [ByteArray] 转换为 [NSData]。 */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
