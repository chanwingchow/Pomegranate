package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

internal data class PickerHsv(val hue: Float, val saturation: Float, val brightness: Float)

internal fun Color.pickerHsv(): PickerHsv {
    val rgb = convert(ColorSpaces.Srgb)
    val high = maxOf(rgb.red, rgb.green, rgb.blue)
    val low = minOf(rgb.red, rgb.green, rgb.blue)
    val delta = high - low
    val hue = when {
        delta == 0f -> 0f
        high == rgb.red -> 60f * (((rgb.green - rgb.blue) / delta) % 6f)
        high == rgb.green -> 60f * ((rgb.blue - rgb.red) / delta + 2f)
        else -> 60f * ((rgb.red - rgb.green) / delta + 4f)
    }
    return PickerHsv((hue + 360f) % 360f, if (high == 0f) 0f else delta / high, high)
}

internal fun colorPoint(x: Float, y: Float, width: Float, height: Float, inset: Float): Pair<Float, Float> =
    ((x - inset) / (width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f) to
        (1f - (y - inset) / (height - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
