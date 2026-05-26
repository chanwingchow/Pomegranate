package com.chow.pomegranate.tool.ocr

import com.moondeveloper.ocr.OcrEngine

/**
 * 创建验证码 Ocr 引擎。
 */
expect fun createCaptchaOcrEngine(
    block: CaptchaOcrEngineConfig.() -> Unit = {},
): OcrEngine

/**
 * 验证码 OCR 引擎配置。
 */
class CaptchaOcrEngineConfig {
    /** 训练数据路径 */
    var dataPath: String = ""
        private set

    /** 识别语言 */
    val language: String = "eng"

    /** 识别限定字符 */
    val charWhiteList: String = "123abcdmnvxz"

    /** 验证码边框宽度 */
    val borderWidth: Int = 3

    /**
     * 配置训练数据路径。
     */
    fun dataPath(path: String) {
        dataPath = path
    }
}