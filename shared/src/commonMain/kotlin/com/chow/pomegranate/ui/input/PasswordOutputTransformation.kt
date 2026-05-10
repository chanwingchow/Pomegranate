package com.chow.pomegranate.ui.input

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * 密码输出转化。
 */
class PasswordOutputTransformation(
    private val mask: Char = '\u2022', // •
): OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        for (i in 0 until length) {
            replace(i, i + 1, mask.toString())
        }
    }
}