package com.chow.pomegranate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** 首页和选课总表共用课程配色、圆角及名称/地点层级。 */
@Composable
internal fun TimetableCourseCard(
    name: String,
    classroom: String?,
    modifier: Modifier = Modifier,
    teacher: String? = null,
    colorKey: String = name,
    wide: Boolean = false,
    selected: Boolean = false,
    otherWeek: Boolean = false,
    count: Int = 1,
    compactMetadata: Boolean = false,
    nameOverride: String? = null,
    classroomOverride: String? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val displayName = courseNameText(name, nameOverride)
    val displayRoom = classroomOverride ?: classroom?.let { classroomText(it) }
    val color = when ((colorKey.hashCode() and Int.MAX_VALUE) % 3) {
        0 -> colors.primaryContainer
        1 -> colors.tertiaryContainer
        else -> colors.secondaryContainer
    }
    val shape = MaterialTheme.shapes.medium
    val containerColor = if (otherWeek) colors.surfaceContainerLow else color
    Box(modifier.semantics(mergeDescendants = true) {
        stateDescription = listOfNotNull(
            "非本周课程".takeIf { otherWeek },
            "同一时段共 $count 门课程，点击查看全部".takeIf { count > 1 },
        ).joinToString("，")
    }) {
        // 叠层只在卡片外缘画提示，不占名称或教室的布局空间。
        for (layer in minOf(count - 1, 2) downTo 1) {
            val shift = (layer + 1).dp
            Box(Modifier.matchParentSize().offset(x = shift, y = -shift)
                .background(containerColor, shape).border(1.dp, colors.outlineVariant, shape))
        }
        Surface(onClick = onClick, modifier = Modifier.fillMaxSize(), shape = shape,
            color = containerColor,
            contentColor = if (otherWeek) colors.onSurfaceVariant else colors.onSurface,
            border = if (selected) BorderStroke(1.5.dp, colors.primary) else null) {
            // Draw on the actual surface, inside its minimum touch target, so narrow phone tiles align too.
            BoxWithConstraints(Modifier.fillMaxSize().then(if (otherWeek) Modifier.drawWithCache {
                val strokeWidth = 1.dp.toPx()
                val inset = strokeWidth / 2
                val outline = shape.createOutline(
                    Size((size.width - strokeWidth).coerceAtLeast(0f), (size.height - strokeWidth).coerceAtLeast(0f)),
                    layoutDirection, this,
                )
                val stroke = Stroke(width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                onDrawWithContent {
                    drawContent()
                    translate(inset, inset) { drawOutline(outline, colors.outline, style = stroke) }
                }
            } else Modifier)) {
                val compactWide = wide && maxHeight < 104.dp
                val smallCell = compactMetadata && maxWidth < 40.dp
                val showRoom = !compactMetadata || maxHeight >= 64.dp
                Column(Modifier.fillMaxSize().padding(if (wide) 8.dp else if (compactMetadata) 2.dp else 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val style = when {
                            wide -> MaterialTheme.typography.titleSmall
                            smallCell -> MaterialTheme.typography.labelSmall
                            else -> MaterialTheme.typography.bodySmall
                        }
                        val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
                        val lines = (maxHeight.value / lineHeight.value).toInt().coerceIn(1, if (wide) 3 else 4)
                        Text(displayName, style = style, fontWeight = FontWeight.SemiBold, maxLines = lines, overflow = TextOverflow.Ellipsis)
                    }
                    displayRoom?.takeIf { showRoom && it.isNotBlank() }?.let {
                        Text(it, style = if (wide) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
                    }
                    if (wide && !compactWide) teacher?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}
