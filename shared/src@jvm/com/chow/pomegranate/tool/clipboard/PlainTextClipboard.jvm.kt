package com.chow.pomegranate.tool.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun ClipEntry.plainText(): String? = asAwtTransferable
    ?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) }
    ?.getTransferData(DataFlavor.stringFlavor) as? String
