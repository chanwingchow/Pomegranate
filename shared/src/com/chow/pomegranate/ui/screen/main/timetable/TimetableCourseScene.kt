package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

internal val LocalTimetableDetailPane = staticCompositionLocalOf { false }
internal const val TIMETABLE_MAIN_METADATA = "pomegranate.timetableMain"
internal const val TIMETABLE_DETAIL_METADATA = "pomegranate.timetableCourse"

@Composable
internal fun rememberTimetableCourseSceneStrategy(): TimetableCourseSceneStrategy {
    val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() >= 1100.dp }
    return remember(wide) { TimetableCourseSceneStrategy(wide) }
}

/** 主页面始终在同一个场景和调用位置，展开详情时只增减右栏，避免主内容跨场景搬移。 */
internal class TimetableCourseSceneStrategy(private val wide: Boolean) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        val detail = last.takeIf { wide && it.metadata[TIMETABLE_DETAIL_METADATA] == true }
        val main = if (detail != null) entries.getOrNull(entries.lastIndex - 1) else last
        if (main?.metadata?.get(TIMETABLE_MAIN_METADATA) != true) return null
        // 返回应只移除详情，仍保留主页面；不能把两者一起从 previousEntries 移走。
        return TimetableSupportingScene(main, detail, entries.dropLast(1))
    }
}

private data class TimetableSupportingScene(
    val main: NavEntry<NavKey>, val detail: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = main.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(main, detail)
    override val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CompositionLocalProvider(LocalTimetableDetailPane provides (detail != null)) { main.Content() }
            }
            detail?.let { entry ->
                Surface(Modifier.width(340.dp).fillMaxHeight().padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                    shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    CompositionLocalProvider(LocalTimetableDetailPane provides true) { entry.Content() }
                }
            }
        }
    }
}
