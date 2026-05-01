package com.chow.pomegranate.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 导航回退栈。
 */
val LocalBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    NavBackStack()
}