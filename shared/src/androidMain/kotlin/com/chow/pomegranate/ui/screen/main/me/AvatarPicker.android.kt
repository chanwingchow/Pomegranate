package com.chow.pomegranate.ui.screen.main.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.net.URI

@Composable
actual fun rememberAvatarPicker(onResult: (AvatarPickResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val directory = File(context.filesDir, "avatars").apply { mkdirs() }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取所选图片")
                // 保留原始格式（PNG/JPEG/WebP），不重编码，避免有损压缩和透明通道丢失
                val target = File(directory, newLocalAvatarFileName(detectImageExtension(bytes)))
                target.writeBytes(bytes)
                AvatarPickResult.Selected(target.toURI().toString())
            }.getOrElse { AvatarPickResult.Failed("头像保存失败，请重试。") } }
            onResult(result)
        }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

actual suspend fun deleteLocalAvatar(uri: String) {
    runCatching { File(URI(uri)).delete() }
}
