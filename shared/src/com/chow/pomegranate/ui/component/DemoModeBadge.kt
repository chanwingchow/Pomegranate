package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.Res
import com.chow.pomegranate.demo_badge
import org.jetbrains.compose.resources.stringResource

/** 根布局右上角的演示标签，不占用页面布局或拦截导航按钮的点击。 */
@Composable
fun DemoModeBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            stringResource(Res.string.demo_badge),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
