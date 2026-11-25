package com.chow.pomegranate.service.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

@Composable
internal actual fun rememberUpdateInstaller(): UpdateInstaller? = remember {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val arch = System.getProperty("os.arch").orEmpty().lowercase()
    val x64 = arch == "amd64" || arch == "x86_64"
    val target = when {
        os.startsWith("windows") && x64 -> UpdateTarget.WindowsX64
        os.startsWith("mac") && (arch == "aarch64" || arch == "arm64") -> UpdateTarget.MacArm64
        os.startsWith("linux") && x64 -> UpdateTarget.LinuxX64
        else -> null
    }
    target?.let(::DesktopUpdateInstaller)
}

private class DesktopUpdateInstaller(override val target: UpdateTarget) : UpdateInstaller {
    override val directory = Path(System.getProperty("user.home"), ".pomegranate", "updates")
    override val openLabel = "打开安装包"

    override suspend fun open(path: Path): String = withContext(Dispatchers.IO) {
        val file = File(path.toString())
        val message = "安装包已下载至\n$path"
        if (!Desktop.isDesktopSupported()) return@withContext message
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) return@withContext message
        try {
            desktop.open(file)
            "已交给系统打开安装包。"
        } catch (_: IOException) {
            runCatching { desktop.open(file.parentFile) }
            message
        }
    }
}
