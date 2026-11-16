package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.ui.graphics.Color
import kotlin.test.*

class ThemeColorTest {
    @Test fun transparentCustomColorsPreserveAllEightDigits() {
        val color = Color(0x00112233)
        assertEquals("00112233", color.toHexString())
        assertEquals(color, "00112233".toColor())
    }
    @Test fun sixDigitColorsAreOpaqueAndHashIsAccepted() {
        assertEquals(Color(0xFF112233), "#112233".toColor())
        assertEquals(Color(0x80112233), "80112233".toColor())
    }
    @Test fun hsvRoundTripRetainsPresetAndCustomColors() {
        listOf(Color.Red, Color.Green, Color.Blue, Color(0xFF29409D), Color(0xFF017F83)).forEach { color ->
            val hsv = color.pickerHsv()
            val restored = Color.hsv(hsv.hue, hsv.saturation, hsv.brightness)
            assertEquals(color.red, restored.red, 1f / 255)
            assertEquals(color.green, restored.green, 1f / 255)
            assertEquals(color.blue, restored.blue, 1f / 255)
        }
    }
    @Test fun grayAndBlackHaveDefinedHueAndSaturation() {
        assertEquals(PickerHsv(0f, 0f, 0f), Color.Black.pickerHsv())
        val gray = Color.Gray.pickerHsv()
        assertEquals(0f, gray.hue)
        assertEquals(0f, gray.saturation)
        assertEquals(Color.Gray.red, gray.brightness)
    }
    @Test fun dragCoordinatesClampToTheColorAreaIncludingItsInset() {
        assertEquals(0f to 1f, colorPoint(-100f, -100f, 200f, 120f, 12f))
        assertEquals(1f to 0f, colorPoint(300f, 200f, 200f, 120f, 12f))
        assertEquals(.5f to .5f, colorPoint(100f, 60f, 200f, 120f, 12f))
    }

}
