package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.runtime.Composable

/** 从平台照片选择器取得并持久化到应用私有存储的结果。 */
sealed interface AvatarPickResult {
    data class Selected(val localUri: String) : AvatarPickResult
    data class Unavailable(val message: String) : AvatarPickResult
    data class Failed(val message: String) : AvatarPickResult
}

/**
 * 创建平台头像选择器。
 *
 * 仅返回本地副本，不会请求或伪造学校服务端的头像上传。
 */
@Composable
expect fun rememberAvatarPicker(onResult: (AvatarPickResult) -> Unit): () -> Unit

/** 删除本地头像副本；失败不阻碍恢复默认头像。 */
expect suspend fun deleteLocalAvatar(uri: String)
