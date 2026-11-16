package com.chow.pomegranate.ui.screen.main.me

import kotlin.random.Random
import kotlin.time.Clock

/** 默认存档扩展名，无法识别原始格式时使用。 */
internal const val DEFAULT_AVATAR_EXTENSION = "png"

/**
 * 生成本地头像副本的文件名。
 *
 * 每次选择都用新的文件名，而不是固定覆盖 `profile.png`：Coil 默认按 URI 作为缓存键，
 * 不包含文件修改时间，固定文件名会让第二次换头像仍然命中旧图。
 *
 * 文件名只包含 ASCII 字符，三端都可安全用于文件系统。
 *
 * @param extension 实际图片格式，不含点号
 */
internal fun newLocalAvatarFileName(extension: String = DEFAULT_AVATAR_EXTENSION): String {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    val suffix = Random.nextInt(from = 0, until = 0x10000).toString(radix = 16).padStart(4, '0')
    // 只保留字母数字，避免把奇怪的后缀带进文件名
    val safeExtension = extension.filter(Char::isLetterOrDigit).ifEmpty { DEFAULT_AVATAR_EXTENSION }

    return "avatar-$timestamp-$suffix.$safeExtension"
}
