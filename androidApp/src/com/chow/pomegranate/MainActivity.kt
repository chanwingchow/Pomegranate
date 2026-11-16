package com.chow.pomegranate

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                platformKoinConfiguration = { androidContext(this@MainActivity) },
                onThemeResolved = ::applyThemeToWindow,
            )
        }
    }

    /**
     * 应用内主题（浅色 / 深色 / 跟随系统）解析出来后同步给窗口。
     *
     * 只按系统深浅决定是不够的，应用内选深色而系统是浅色时会出现两个问题：
     *  - 窗口最底层仍是浅色，启动窗口、对话框和页面转场会闪一下白；
     *  - `enableEdgeToEdge()` 按「系统浅色」把系统栏图标设成深色，
     *    压在纯黑界面上等于看不见（实测状态栏亮像素 0%）。
     */
    private fun applyThemeToWindow(background: Color, isDark: Boolean) {
        window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
