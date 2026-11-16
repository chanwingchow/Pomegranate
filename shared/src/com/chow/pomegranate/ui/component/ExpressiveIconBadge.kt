package com.chow.pomegranate.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

/** 共享的小型标识：主题配色、Material 形状、按压时平滑变圆。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconBadge(
    icon: ImageVector,
    variant: Int,
    pressed: Boolean,
    modifier: Modifier = Modifier.size(40.dp),
    iconSize: Dp = 22.dp,
) {
    val polygon = when (variant % 4) {
        0 -> MaterialShapes.Cookie4Sided
        1 -> MaterialShapes.Clover4Leaf
        2 -> MaterialShapes.Gem
        else -> MaterialShapes.Cookie9Sided
    }
    val morph = remember(polygon) { Morph(polygon, MaterialShapes.Circle) }
    val progress by animateFloatAsState(
        if (pressed) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "Icon shape",
    )
    val shape = remember(morph, progress) { BadgeShape(morph, progress.coerceIn(0f, 1f)) }
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (variant % 3) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(modifier, shape = shape, color = container, contentColor = content) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class BadgeShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}
