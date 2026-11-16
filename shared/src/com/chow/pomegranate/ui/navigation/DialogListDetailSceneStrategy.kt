package com.chow.pomegranate.ui.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.*

/** 优先使用 Material ListDetail；窗口不能并排时，同一详情目的地呈现为弹窗。 */
internal class DialogListDetailSceneStrategy(private val listDetail: SceneStrategy<NavKey>) : SceneStrategy<NavKey> {
    private val dialog = DialogSceneStrategy<NavKey>()

    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (entries.lastOrNull()?.metadata?.get(MetadataKey) != true) return null
        return with(listDetail) { calculateScene(entries) } ?: with(dialog) { calculateScene(entries) }
    }

    companion object {
        private const val MetadataKey = "pomegranate.dialogListDetail"
        fun metadata(): Map<String, Any> = mapOf(MetadataKey to true) +
            DialogSceneStrategy.dialog(DialogProperties(usePlatformDefaultWidth = false))
    }
}
