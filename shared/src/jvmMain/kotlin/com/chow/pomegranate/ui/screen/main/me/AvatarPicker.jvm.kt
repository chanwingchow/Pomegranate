package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** JVM 端从系统文件选择器复制图片到应用专用目录。 */
@Composable
actual fun rememberAvatarPicker(onResult: (AvatarPickResult) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember(scope, onResult) {
        {
            val chooser = JFileChooser().apply {
                dialogTitle = "选择本地头像"
                fileSelectionMode = JFileChooser.FILES_ONLY
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val source = chooser.selectedFile
                scope.launch {
                    onResult(withContext(Dispatchers.IO) {
                        runCatching {
                            require(source.length() <= MAX_AVATAR_BYTES) { "头像图片不能超过 10 MB。" }
                            val directory = File(System.getProperty("user.home"), ".pomegranate/avatars")
                            directory.mkdirs()
                            // 保留原始格式（PNG/JPEG/WebP），只复制不重编码
                            val extension = source.inputStream().use(::detectImageExtension)
                            val target = File(directory, newLocalAvatarFileName(extension))
                            source.copyTo(target, overwrite = true)
                            AvatarPickResult.Selected(target.toURI().toString())
                        }.getOrElse { AvatarPickResult.Failed(it.message ?: "头像保存失败，请重试。") }
                    })
                }
            }
        }
    }
}

actual suspend fun deleteLocalAvatar(uri: String) {
    runCatching { File(URI(uri)).delete() }
}

private const val MAX_AVATAR_BYTES = 10L * 1024 * 1024
