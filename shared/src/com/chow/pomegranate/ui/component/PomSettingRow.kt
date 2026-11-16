package com.chow.pomegranate.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 与“我的”页共用整行点击、图标按压反馈和主题容器。 */
@Composable
fun PomSettingRow(title: String, icon: ImageVector, variant: Int, modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Surface(onClick, modifier.fillMaxWidth(), interactionSource = interactions,
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconBadge(icon, variant, pressed, Modifier.size(32.dp), iconSize = 20.dp)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            trailing()
        }
    }
}
