package com.chow.pomegranate.demo

import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.*

class MockCaptchaImageTest {
    @Test fun captchaIsAValidReadablePngRatherThanOnlyHavingAPngHeader() = runTest {
        val bytes = MockAcademicAffairs(0).auth.getCaptcha()
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertNotNull(image)
        assertTrue(image.width >= 80 && image.height >= 30)
        val colors = (0 until image.width).flatMap { x -> (0 until image.height).map { y -> image.getRGB(x, y) } }.toSet()
        assertTrue(colors.size > 1)
    }
}
