package com.chow.pomegranate.ui.screen.main.timetable

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.screen.main.MainRoute
import kotlin.test.*

class TimetableCourseSceneTest {
    private val scope = SceneStrategyScope<NavKey>()
    private val main = NavEntry<NavKey>(MainRoute, metadata = mapOf(TIMETABLE_MAIN_METADATA to true)) {}
    private val detail = NavEntry<NavKey>(TimetableCourseRoute(StudyDetailRoute("test", "课程", "")),
        metadata = mapOf(TIMETABLE_DETAIL_METADATA to true)) {}

    @Test fun openingAndClosingThePaneKeepsTheMainSceneAndItsBackTarget() {
        with(TimetableCourseSceneStrategy(true)) {
            val before = scope.calculateScene(listOf(main))!!
            val expanded = scope.calculateScene(listOf(main, detail))!!
            val closed = scope.calculateScene(expanded.previousEntries)!!
            assertEquals(before.key, expanded.key)
            assertEquals(before.key, closed.key)
            assertSame(main, expanded.entries.first())
            assertEquals(listOf(main), expanded.previousEntries)
            assertEquals(listOf(main), closed.entries)
        }
    }

    @Test fun resizingUsesTheSameMainSceneWhileNarrowDetailsGoToTheBottomSheet() {
        val wide = with(TimetableCourseSceneStrategy(true)) { scope.calculateScene(listOf(main))!! }
        with(TimetableCourseSceneStrategy(false)) {
            assertEquals(wide.key, scope.calculateScene(listOf(main))!!.key)
            assertNull(scope.calculateScene(listOf(main, detail)))
        }
    }
}
