package com.chow.pomegranate.tool.ocr

import com.moondeveloper.ocr.NoOpOcrEngine
import com.moondeveloper.ocr.OcrEngine

actual fun createCaptchaOcrEngine(
    block: CaptchaOcrEngineConfig.() -> Unit,
): OcrEngine {
    return NoOpOcrEngine()
}