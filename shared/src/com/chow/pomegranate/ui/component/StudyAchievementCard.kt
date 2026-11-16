package com.chow.pomegranate.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.repository.credit
import com.chow.pomegranate.ui.navigation.LocalBackStack

/** 概览只保留数值和达标状态，避免嵌套进度面板撑大列表卡片。 */
@Composable
internal fun StudyAchievementCard(record: StudyRecord, icon: ImageVector, index: Int, onClick: (() -> Unit)?) {
    val result = record.achievement ?: return
    val selected = LocalBackStack.current.lastOrNull().studyDetailRoute()?.id == record.id
    val content: @Composable (Boolean) -> Unit = { pressed ->
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpressiveIconBadge(icon, index, pressed, Modifier.size(32.dp), iconSize = 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    if (result.unit == "学分") Text(record.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onClick != null) Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text("${credit(result.earned)} / ${credit(result.required)} ${result.unit}", style = MaterialTheme.typography.titleMedium)
                AchievementStatus(result)
            }
        }
    }
    if (onClick != null) ExpressiveActionCard(onClick, selected = selected) { content(it) }
    else Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) { content(false) }
}

@Composable
internal fun AchievementStatus(result: StudyAchievement) {
    val completed = result.completed
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.small,
        color = when (completed) { true -> colors.secondaryContainer; false -> colors.tertiaryContainer; null -> colors.surfaceContainerHigh },
        contentColor = when (completed) { true -> colors.onSecondaryContainer; false -> colors.onTertiaryContainer; null -> colors.onSurfaceVariant }) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (completed == true) Icons.Rounded.CheckCircle else Icons.Rounded.PendingActions, null, Modifier.size(16.dp))
            Text(when (completed) { true -> "已达要求"; false -> "未达要求"; null -> "进度待确认" }, style = MaterialTheme.typography.labelMedium)
        }
    }
}
