package com.chow.pomegranate.tool.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlainTextClipboardTest {
    @Test fun otpUriAndUnicodeTextSurviveNativeClipConversion() = runTest {
        val clipboard = MemoryClipboard()
        for (text in listOf("123456", "otpauth://totp/校园:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=校园", "第一行\n第二行 🔐", "")) {
            clipboard.writePlainText(text)
            assertEquals(text, clipboard.readPlainText())
        }
    }

    @Test fun emptyClipboardDoesNotInventText() = runTest {
        assertNull(MemoryClipboard().readPlainText())
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test fun nonTextClipboardEntryIsIgnored() = runTest {
        val nonText = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any = throw UnsupportedFlavorException(flavor)
        }
        assertNull(MemoryClipboard(ClipEntry(nonText)).readPlainText())
    }
}

private class MemoryClipboard(private var entry: ClipEntry? = null) : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = entry
    override suspend fun setClipEntry(clipEntry: ClipEntry?) { entry = clipEntry }
}
