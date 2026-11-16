package com.chow.pomegranate.ui.screen.transcript

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.*
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.navigation.DialogListDetailSceneStrategy
import com.chow.pomegranate.ui.screen.main.MainRoute
import kotlinx.serialization.json.Json
import kotlin.test.*

class TranscriptDetailSceneStrategyTest {
    @Test fun compactUsesDialogRetainingListAndLargeUsesProvidedMaterialScene() {
        val list = NavEntry<NavKey>(TranscriptRoute) {}
        val detail = NavEntry<NavKey>(TranscriptDetailRoute(StudyDetailRoute("grade", "程序设计"), "54"),
            metadata = DialogListDetailSceneStrategy.metadata()) {}
        val entries = listOf(list, detail)
        val scope = SceneStrategyScope<NavKey>()
        val compactMaterial = object : SceneStrategy<NavKey> {
            override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? = null
        }
        val dialog = with(DialogListDetailSceneStrategy(compactMaterial)) { scope.calculateScene(entries) }!!
        assertTrue(dialog is OverlayScene<*>)
        assertSame(detail, dialog.entries.single())
        assertEquals(listOf(list), dialog.previousEntries)
        val wideScene = with(SinglePaneSceneStrategy<NavKey>()) { scope.calculateScene(entries) }
        val wideMaterial = object : SceneStrategy<NavKey> {
            override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey> = wideScene
        }
        with(DialogListDetailSceneStrategy(wideMaterial)) {
            assertSame(wideScene, scope.calculateScene(entries))
            assertNull(scope.calculateScene(listOf(NavEntry<NavKey>(MainRoute) {})))
        }
    }

    @Test fun restoredDetailKeepsAttemptIdentityScoreAndFullSnapshot() {
        val original = TranscriptDetailRoute(StudyDetailRoute("CS101:term:补考", "程序设计", "补考"), "76")
        assertEquals(original, Json.decodeFromString<TranscriptDetailRoute>(Json.encodeToString(original)))
    }
}
