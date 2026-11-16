package com.chow.pomegranate.tool.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun ClipEntry.plainText(): String? = getPlainText()
