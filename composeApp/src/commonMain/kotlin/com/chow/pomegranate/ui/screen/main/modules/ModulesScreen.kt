package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/**
 * 模块页导航入口。
 */
fun EntryProviderScope<NavKey>.modulesEntry() {
    entry<ModulesRoute> {
        ModulesScreen()
    }
}

/**
 * 模块页路由。
 */
@Serializable
data object ModulesRoute : NavKey

/**
 * 模块页。
 */
@Composable
private fun ModulesScreen() {
    ModulesContent()
}

@Preview
@Composable
private fun ModulesContentPreview() {
    PomegranateExpressiveTheme {
        ModulesContent()
    }
}

/**
 * 模块页内容。
 */
@Composable
private fun ModulesContent(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            contentPadding = innerPadding + PaddingValues(16.dp),
        ) {
            items(Module.entries) { module ->
                val moduleLabel = stringResource(module.label)

                ModuleItem(
                    onClick = { },
                    headlineContent = {
                        Text(
                            moduleLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        when (module) {
                            else -> Unit
                        }
                        Text("Hello")
                    },
                    trailingContent = {
                        Icon(
                            module.icon,
                            contentDescription = moduleLabel,
                            tint = MaterialTheme.colorScheme.inversePrimary,
                        )
                    },
                )
            }
        }
    }
}

/**
 * 模块项。
 */
@Composable
private fun ModuleItem(
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        ListItem(
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            tonalElevation = 1.dp,
        )
    }
}