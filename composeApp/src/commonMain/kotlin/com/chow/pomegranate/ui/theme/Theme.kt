package com.chow.pomegranate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.rememberDynamicMaterialThemeState

/**
 * 小石榴富有表现力主题。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomegranateExpressiveTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    DynamicMaterialExpressiveTheme(
        state = rememberDynamicMaterialThemeState(
            // 她喜欢蓝色，但我不确定是不是这款
            primary = Color(0xFF2642A4),
            isDark = isDark,
            isAmoled = true,
        ),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}