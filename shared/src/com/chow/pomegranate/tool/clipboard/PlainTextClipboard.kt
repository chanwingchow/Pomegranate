package com.chow.pomegranate.tool.clipboard

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

// Compose 1.12 exposes Clipboard in common code, but text conversion is still platform specific.
internal suspend fun Clipboard.writePlainText(text: String) = setClipEntry(plainTextClipEntry(text))

internal suspend fun Clipboard.readPlainText(): String? = getClipEntry()?.plainText()

internal expect fun plainTextClipEntry(text: String): ClipEntry

internal expect suspend fun ClipEntry.plainText(): String?
