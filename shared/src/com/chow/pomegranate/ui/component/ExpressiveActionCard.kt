package com.chow.pomegranate.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** 轻量按压反馈，与模块入口共享相同的形状、配色与动效语言。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.(pressed: Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) .98f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "Card press",
    )
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        MaterialTheme.motionScheme.fastEffectsSpec(), label = "Card selection")
    // 按压波纹与选中态都使用主题色，避免默认 onSurface 灰色波纹在松开时突变。
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = colors.primary)) {
        val cardModifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
        if (onLongClick != null) Card(
            modifier = cardModifier.combinedClickable(interactionSource = interactions, indication = ripple(), role = Role.Button,
                onClick = onClick, onLongClickLabel = "多选课程", onLongClick = onLongClick),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { content(pressed) }
        else Card(
            onClick = onClick,
            modifier = cardModifier,
            interactionSource = interactions,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { content(pressed) }
    }
}
