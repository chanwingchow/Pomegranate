package com.chow.pomegranate.ui.component

import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupPositionProvider

/**
 * 工具提示。
 */
@Composable
fun PomTooltipBox(
    tooltip: @Composable TooltipScope.() -> Unit,
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        // 在下方弹出
        positioning = TooltipAnchorPosition.Below,
    ),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}