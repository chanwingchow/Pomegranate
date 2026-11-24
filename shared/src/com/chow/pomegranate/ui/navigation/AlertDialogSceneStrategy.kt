package com.chow.pomegranate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** 保留下方场景，由目标页的 Material3 AlertDialog 创建唯一的弹窗。 */
internal class AlertDialogSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (entries.size < 2 || entry.metadata[AlertDialogMetadata] != true) return null
        return AlertDialogScene(entry, entries.dropLast(1))
    }

    companion object {
        private const val AlertDialogMetadata = "pomegranate.alertDialog"

        fun alertDialog(): Map<String, Any> = mapOf(AlertDialogMetadata to true)
    }
}

private data class AlertDialogScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> = previousEntries
    override val content: @Composable () -> Unit = { entry.Content() }
}
