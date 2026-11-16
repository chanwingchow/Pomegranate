package com.chow.pomegranate

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.navigation.FullWidthOverviewSceneStrategy
import com.chow.pomegranate.ui.screen.feature.CourseSelectionRoute
import com.chow.pomegranate.ui.screen.main.MainRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class FullWidthOverviewSceneStrategyTest {
    @Test fun overviewRetainsItsEntryAndBackTargetAndDelegatesDetailsToAdaptiveScene() {
        val scope = SceneStrategyScope<NavKey>()
        val main = NavEntry<NavKey>(MainRoute) {}
        val overview = NavEntry<NavKey>(CourseSelectionRoute, metadata = FullWidthOverviewSceneStrategy.metadata()) {}
        val detail = NavEntry<NavKey>(StudyDetailRoute("course", "课程")) {}
        with(FullWidthOverviewSceneStrategy<NavKey>()) {
            val scene = scope.calculateScene(listOf(main, overview))!!
            assertSame(overview, scene.entries.single())
            assertEquals(listOf(main), scene.previousEntries)
            assertNull(scope.calculateScene(listOf(main, overview, detail)))
            assertEquals(scene.key, scope.calculateScene(listOf(main, overview))!!.key)
        }
    }
}
