package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.app_name

/**
 * 顶部导航栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    shadowElevation: Dp = 1.dp,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(0.8f),
        shadowElevation = shadowElevation,
    ) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            contentPadding = contentPadding,
        )
    }
}

@Preview
@Composable
private fun PomTopAppBarPreview() {
    PomegranateExpressiveTheme {
        PomTopAppBar(
            title = {
                Text(
                    stringResource(Res.string.app_name)
                )
            },
        )
    }
}