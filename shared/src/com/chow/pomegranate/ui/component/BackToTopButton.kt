package com.chow.pomegranate.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

@Composable
fun BackToTopButton(state: LazyGridState) {
    val visible by remember(state) { derivedStateOf { state.canScrollBackward && state.lastScrolledBackward } }
    val scope = rememberCoroutineScope()
    BackToTopButton(visible) { scope.launch { state.animateScrollToItem(0) } }
}

@Composable
fun BackToTopButton(state: LazyListState) {
    val visible by remember(state) { derivedStateOf { state.canScrollBackward && state.lastScrolledBackward } }
    val scope = rememberCoroutineScope()
    BackToTopButton(visible) { scope.launch { state.animateScrollToItem(0) } }
}

@Composable
fun BackToTopButton(state: LazyStaggeredGridState) {
    val visible by remember(state) { derivedStateOf { state.canScrollBackward && state.lastScrolledBackward } }
    val scope = rememberCoroutineScope()
    BackToTopButton(visible) { scope.launch { state.animateScrollToItem(0) } }
}

@Composable
fun BackToTopButton(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
        SmallFloatingActionButton(onClick = onClick) { Icon(Icons.Rounded.ArrowUpward, "回到顶部") }
    }
}
