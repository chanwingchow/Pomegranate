package com.chow.pomegranate.ui.component

import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.PopupPositionProvider
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.app_name

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
    state: TooltipState = rememberTooltipState(),
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = state,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        focusable = focusable,
        enableUserInput = enableUserInput,
        hasAction = hasAction,
        content = content,
    )
}

@Preview
@Composable
private fun PomTooltipBoxPreview() {
    PomegranateExpressiveTheme {
        PomTooltipBox(
            tooltip = {
                Text(stringResource(Res.string.app_name))
            },
            state = rememberTooltipState(initialIsVisible = true),
            content = {},
        )
    }
}