package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.tool.ocr.createCaptchaOcrEngine
import com.moondeveloper.ocr.OcrEngine
import io.ktor.utils.io.readText
import kotlinx.coroutines.runBlocking
import kotlinx.io.SystemLineSeparator
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 教务系统测试。
 */
class AcademicAffairsTest {
    private lateinit var academicAffairs: AcademicAffairs
    private lateinit var captchaOcrEngine: OcrEngine
    private lateinit var username: String
    private lateinit var password: String

    @BeforeTest
    fun setUp() {
        academicAffairs = AcademicAffairs()
        captchaOcrEngine = createCaptchaOcrEngine {
            dataPath("src\\commonTest\\resources\\tessdata")
        }

        // 读取 local.properties 里面的用户配置
        // user.name=学号
        // user.password=密码
        val properties = SystemFileSystem.source(Path("../local.properties"))
            .buffered()
            .readText()
            .split(SystemLineSeparator)
            .filter { it.startsWith("user") }
            .associate {
                val (key, value) = it.split("=")
                key.trim() to value.trim()
            }

        username = properties["user.name"]!!
        password = properties["user.password"]!!
    }

    /**
     * 登录测试。
     */
    @Test
    fun login() = runBlocking {
        var count = 5

        while (count-- > 0) {
            // 获取验证码
            val bytes = academicAffairs.auth.getCaptcha()

            // 识别验证码
            val captcha = captchaOcrEngine.recognize(bytes)

            // 登录
            val param = LoginParam(
                username = username,
                password = password,
                captcha = captcha.fullText,
            )
            val loginResult = academicAffairs.auth.login(param)

            if (loginResult is LoginResult.Fail.CaptchaError) {
                // 验证码错误重试
                continue
            }

            assertTrue { loginResult is LoginResult.Success }.also { academicAffairs.auth.logout() }
            return@runBlocking
        }

        assertTrue { false }
    }
}