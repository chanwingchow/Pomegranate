package com.chow.pomegranate.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.hide_password
import pomegranate.composeapp.generated.resources.show_password

/**
 * 可见性图标按钮。
 */
@Composable
fun VisibilityIconButton(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(
        checked = visible,
        onCheckedChange = { onVisibleChange(it) },
        modifier = modifier,
    ) {
        Icon(
            if (visible) {
                Icons.TwoTone.VisibilityOff
            } else {
                Icons.TwoTone.Visibility
            },
            contentDescription = stringResource(
                if (visible) {
                    Res.string.hide_password
                } else {
                    Res.string.show_password
                },
            ),
        )
    }
}

@Preview
@Composable
private fun VisibilityIconButtonPreview() {
    PomegranateExpressiveTheme {
        VisibilityIconButton(
            visible = true,
            onVisibleChange = {},
        )
    }
}