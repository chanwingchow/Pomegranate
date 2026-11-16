package com.chow.pomegranate.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用级 Snackbar 状态。
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState is not provided.")
}

/** 页面关闭后仍可完成用户反馈，生命周期由应用根组合管理。 */
val LocalAppCoroutineScope = staticCompositionLocalOf<kotlinx.coroutines.CoroutineScope?> { null }
