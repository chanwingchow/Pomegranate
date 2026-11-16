package com.chow.pomegranate.ui.screen.feature

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.*
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.DialogListDetailSceneStrategy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class AdaptiveStudyDetailTest {
    @Test fun planSnapshotRestoresAllCourseFieldsAndSwitchingCoursesReplacesOnlyDetail() = runTest {
        val catalog = StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(0))
            .load(AcademicFeature.AcademicPlan, Semester.now())
        val records = catalog.groups.first().records
        val stack = mutableListOf<NavKey>(AcademicPlanRoute)
        stack.openAdaptiveStudyDetail(records[0].detail)
        val route = stack.last() as AdaptiveStudyDetailRoute
        assertEquals(route, Json.decodeFromString<AdaptiveStudyDetailRoute>(Json.encodeToString(route)))
        assertEquals(records[0].detail.sections, route.detail.sections)
        assertTrue(route.detail.sections.flatMap { it.fields }.any { it.label == "开课单位" })
        assertEquals(records[0].detail.id, stack.last().studyDetailRoute()?.id)
        stack.openAdaptiveStudyDetail(records[1].detail)
        assertEquals(2, stack.size)
        assertEquals(records[1].detail, (stack.last() as AdaptiveStudyDetailRoute).detail)
        stack.removeLast()
        assertEquals(listOf<NavKey>(AcademicPlanRoute), stack)
    }

    @Test fun compactDialogRetainsPlanListAndLargeDelegatesToMaterialListDetail() {
        val list = NavEntry<NavKey>(AcademicPlanRoute) {}
        val detail = NavEntry<NavKey>(AdaptiveStudyDetailRoute(StudyDetailRoute("plan", "程序设计")), metadata = DialogListDetailSceneStrategy.metadata()) {}
        val entries = listOf(list, detail)
        val scope = SceneStrategyScope<NavKey>()
        val compact = object : SceneStrategy<NavKey> {
            override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? = null
        }
        val dialog = with(DialogListDetailSceneStrategy(compact)) { scope.calculateScene(entries) }!!
        assertIs<OverlayScene<NavKey>>(dialog)
        assertEquals(listOf(list), dialog.previousEntries)
        assertEquals(listOf(list), dialog.overlaidEntries)
        assertSame(detail, dialog.entries.single())
        val wideScene = with(SinglePaneSceneStrategy<NavKey>()) { scope.calculateScene(entries) }
        val wide = object : SceneStrategy<NavKey> {
            override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>) = wideScene
        }
        with(DialogListDetailSceneStrategy(wide)) { assertSame(wideScene, scope.calculateScene(entries)) }
    }
}
