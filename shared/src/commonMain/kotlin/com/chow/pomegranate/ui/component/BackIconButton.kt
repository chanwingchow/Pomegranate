package com.chow.pomegranate.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chow.pomegranate.Res
import com.chow.pomegranate.nav_before
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource

/**
 * 返回按钮。
 */
@Composable
fun BackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.NavigateBefore,
            contentDescription = stringResource(Res.string.nav_before),
        )
    }
}

@Preview
@Composable
private fun BackIconButtonPreview() {
    PomegranateExpressiveTheme {
        BackIconButton(
            onClick = {},
        )
    }
}