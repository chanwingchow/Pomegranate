package com.chow.pomegranate.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用级 Snackbar 状态。
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState is not provided.")
}
