package com.chow.pomegranate.ui.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.scene.SinglePaneSceneStrategy

/** 总览没有选中详情时铺满窗口；存在详情时交回 Material 的自适应列表详情场景。 */
internal class FullWidthOverviewSceneStrategy<T : Any> : SceneStrategy<T> {
    private val singlePane = SinglePaneSceneStrategy<T>()

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.lastOrNull()?.metadata?.get(MetadataKey) != true) return null
        return with(singlePane) { calculateScene(entries) }
    }

    companion object {
        private const val MetadataKey = "pomegranate.fullWidthOverview"
        fun metadata(): Map<String, Any> = mapOf(MetadataKey to true)
    }
}
