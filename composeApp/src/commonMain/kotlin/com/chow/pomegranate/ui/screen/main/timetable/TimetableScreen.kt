package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.serialization.Serializable

/**
 * 课表页导航入口。
 */
fun EntryProviderScope<NavKey>.timetableEntry(
    hazeState: HazeState,
    floatingToolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
) {
    entry<TimetableRoute> {
        TimetableScreen(
            hazeState = hazeState,
            toolbarExpanded = floatingToolbarExpanded,
            onToolbarExpandedChange = onToolbarExpandedChange,
        )
    }
}

/**
 * 课表页路由。
 */
@Serializable
data object TimetableRoute: NavKey

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableScreen(
    hazeState: HazeState,
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
) {
    Scaffold { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { onToolbarExpandedChange(true) },
                    onCollapse = { onToolbarExpandedChange(false) },
                )
                .hazeSource(state = hazeState),
            contentPadding = innerPadding,
        ) {
            items(100, key = { it }) {
                Text("$it")
            }
        }
    }
}