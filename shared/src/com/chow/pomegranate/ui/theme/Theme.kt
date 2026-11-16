package com.chow.pomegranate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import org.koin.compose.koinInject

/** 默认主题色。 */
internal val DEFAULT_PRIMARY_COLOR = Color(0xFF2642A4)

/**
 * 把 RGB 十六进制字符串还原为颜色。
 *
 * 不能用 `Color(ULong)`：它要求打包后的 64 位值，传 24 位 RGB 会让颜色空间字段越界崩溃。
 */
private fun String.toThemeColor(): Color? {
    val value = removePrefix("#").toLongOrNull(radix = 16) ?: return null
    if (value !in 0x00000000..0xFFFFFFFF) return null

    // 6 位按不透明处理，8 位含透明度
    val alpha = if (value <= 0xFFFFFF) 0xFF else ((value shr 24) and 0xFF).toInt()

    return Color(
        red = ((value shr 16) and 0xFF).toInt(),
        green = ((value shr 8) and 0xFF).toInt(),
        blue = (value and 0xFF).toInt(),
        alpha = alpha,
    )
}

/**
 * 小石榴富有表现力主题。
 *
 * 主题模式与主题色取自本地偏好设置，未设置时使用默认值。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomegranateExpressiveTheme(
    onThemeResolved: ((background: Color, isDark: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val repository = koinInject<AppPreferenceRepository>()

    // 已保存的主题模式
    val themeMode by remember(repository) {
        repository.observe(PreferenceKeys.THEME_MODE)
    }.collectAsState(initial = null)

    // 已保存的主题色
    val primaryColorValue by remember(repository) {
        repository.observe(PreferenceKeys.PRIMARY_COLOR)
    }.collectAsState(initial = null)

    val isDark = when (ThemeMode.entries.getOrNull(themeMode?.toIntOrNull() ?: -1)) {
        // 深色
        ThemeMode.Dark -> true
        // 浅色
        ThemeMode.Light -> false
        // 跟随系统
        else -> isSystemInDarkTheme()
    }

    // 主题色以 RGB 十六进制保存，按通道构造
    val primaryColor = primaryColorValue?.toThemeColor() ?: DEFAULT_PRIMARY_COLOR

    DynamicMaterialExpressiveTheme(
        state = rememberDynamicMaterialThemeState(
            primary = primaryColor,
            isDark = isDark,
            isAmoled = true,
        ),
        motionScheme = MotionScheme.expressive(),
    ) {
        // 把「应用内实际生效」的主题交给平台层（Android 拿去设置窗口底色和系统栏图标）。
        // 只按系统深浅来设是不够的：应用内选深色而系统是浅色时，
        // 窗口最底层仍是浅色，启动窗口和页面转场就会闪一下白。
        val background = MaterialTheme.colorScheme.background
        LaunchedEffect(background, isDark) {
            onThemeResolved?.invoke(background, isDark)
        }

        content()
    }
}
