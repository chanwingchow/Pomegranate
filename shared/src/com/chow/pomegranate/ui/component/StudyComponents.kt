package com.chow.pomegranate.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.serialization.Serializable
import coil3.compose.AsyncImage
import com.chow.pomegranate.ui.screen.feature.TeacherSearchRoute

@Serializable
data class StudyField(val label: String, val value: String, val teacherQuery: String? = null, val originalValue: String? = null,
    val classroomSchedule: Boolean = false, val applyReplacements: Boolean = true, val courseLabel: Boolean = false)

@Serializable
data class StudySection(val title: String, val fields: List<StudyField>)

@Serializable
data class StudyAction(val id: String, val name: String, val category: String, val drop: Boolean = false,
    val courseCode: String = "", val credits: Double = 0.0, val teacher: String = "", val ownerKey: String = "")

/** 保存可恢复的详情快照；需二次请求的教师/活动仅传标识。 */
@Serializable
data class StudyDetailRoute(
    val id: String,
    val title: String,
    val summary: String = "",
    val sections: List<StudySection> = emptyList(),
    val teacherId: String? = null,
    val eventId: Int? = null,
    val action: StudyAction? = null,
    val academicProgress: StudyAcademicProgress? = null,
) : NavKey

/** 手机弹窗、大屏列表详情共用同一份可恢复的资料快照。 */
@Serializable
data class AdaptiveStudyDetailRoute(val detail: StudyDetailRoute) : NavKey

internal fun NavKey?.studyDetailRoute(): StudyDetailRoute? = when (this) {
    is StudyDetailRoute -> this
    is AdaptiveStudyDetailRoute -> detail
    else -> null
}

data class StudyRecord(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val supporting: String = "",
    val value: String = "",
    val progress: Float? = null,
    val detail: StudyDetailRoute,
    val imageUrl: String? = null,
    val achievement: StudyAchievement? = null,
    val tags: List<StudyRecordTag> = emptyList(),
)

data class StudyRecordTag(val text: String, val isScore: Boolean = false)

data class StudyAchievement(val earned: Double?, val required: Double?, val unit: String = "分") {
    val completed: Boolean? get() = if (earned == null || required == null) null else earned >= required
    val remaining: Double? get() = if (earned == null || required == null) null else (required - earned).coerceAtLeast(0.0)
}

@Serializable
data class StudyProgressCourse(
    val id: String, val name: String, val attribute: String, val credits: Double,
    val earnedCredits: Double?, val suggestedSemester: String, val exemption: String?,
) {
    val completed: Boolean? get() = earnedCredits?.let { it >= credits }
}

@Serializable
data class StudyAcademicProgress(val earned: Double?, val required: Double?, val courses: List<StudyProgressCourse>)

/** 同一列表里切换选择时替换详情，避免堆叠多层详情返回历史。 */
fun MutableList<NavKey>.openStudyDetail(route: StudyDetailRoute) {
    if (lastOrNull().studyDetailRoute() != null) removeAt(lastIndex)
    add(route)
}

fun MutableList<NavKey>.openAdaptiveStudyDetail(route: StudyDetailRoute) {
    if (lastOrNull().studyDetailRoute() != null) removeAt(lastIndex)
    add(AdaptiveStudyDetailRoute(route))
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isStudyTwoPane(): Boolean {
    val value = LocalListDetailSceneScope.current?.scaffoldTransitionScope?.scaffoldStateTransition?.targetState
    return value?.get(ListDetailPaneScaffoldRole.List) == PaneAdaptedValue.Expanded &&
        value[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
}

@Composable
fun StudyDetailPlaceholder() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.TouchApp, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text("选择一项查看详情", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StudyRecordCard(record: StudyRecord, icon: ImageVector, index: Int, interactive: Boolean = true, onClick: () -> Unit) {
    if (record.achievement != null) {
        StudyAchievementCard(record, icon, index, onClick.takeIf { interactive })
        return
    }
    val selected = LocalBackStack.current.lastOrNull().studyDetailRoute()?.id == record.detail.id
    ExpressiveActionCard(onClick = onClick, selected = selected) { pressed ->
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp)) {
                    ExpressiveIconBadge(icon, index, pressed)
                    record.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        AsyncImage(url, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(MaterialTheme.shapes.medium))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(studyTitleText(record.detail), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (record.subtitle.isNotBlank()) Text(record.subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (record.value.isNotBlank() && record.progress == null && LocalDensity.current.fontScale <= 1.3f) {
                    Text(record.value, Modifier.widthIn(max = 104.dp), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (record.tags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                record.tags.forEach { tag ->
                    Surface(shape = MaterialTheme.shapes.small,
                        color = if (tag.isScore) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (tag.isScore) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer) {
                        Text(tag.text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (record.supporting.isNotBlank()) Text(record.supporting, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (record.value.isNotBlank() && (record.progress != null || LocalDensity.current.fontScale > 1.3f)) Text(record.value, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            record.progress?.let { StudyProgress(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyProgress(value: Float) {
    val progress by animateFloatAsState(value.coerceIn(0f, 1f), MaterialTheme.motionScheme.defaultEffectsSpec(), label = "Study progress")
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun StudySearch(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    PomTextField(value, onValueChange, modifier.fillMaxWidth(), placeholder = placeholder,
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Close, "清除搜索") } })
}

@Composable
fun StudyTopBar(content: @Composable ColumnScope.() -> Unit) {
    // 导航与 Tab 共用一层投影，避免 Tab 的上边缘再画一道阴影。
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(content = content)
    }
}

@Composable
fun StudyTabs(labels: List<String>, selected: Int, scrollable: Boolean = false, onSelect: (Int) -> Unit) {
    val tabs: @Composable () -> Unit = {
        labels.forEachIndexed { index, label ->
            Tab(selected == index, onClick = { onSelect(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text(label, maxLines = 1) })
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (scrollable || LocalDensity.current.fontScale > 1.3f) PrimaryScrollableTabRow(selectedTabIndex = selected,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp, divider = {}, tabs = tabs)
        else PrimaryTabRow(selectedTabIndex = selected, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface, divider = {}, tabs = tabs)
    }
}

@Composable
fun StudySectionContent(section: StudySection) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section.title.isNotBlank()) Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BoxWithConstraints {
                    val stackLabels = maxWidth < 260.dp || LocalDensity.current.fontScale > 1.3f
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        section.fields.filter { it.value.isNotBlank() }.forEach { field ->
                            if (stackLabels || field.label.isBlank() || field.value.length > 70) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (field.label.isNotBlank()) Text(if (field.courseLabel) courseNameText(field.label) else field.label, style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    StudyFieldValue(field)
                                }
                            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = if (field.label in classroomFieldLabels || field.classroomSchedule) Alignment.Top else Alignment.CenterVertically) {
                                Text(if (field.courseLabel) courseNameText(field.label) else field.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                StudyFieldValue(field, Modifier.weight(2f))
                            }
                        }
                    }
                }
            }
        }
    }
}


/** 接口只给姓名时保留候选列表，让用户辨别重名教师。 */
@Composable
internal fun StudyFieldValue(field: StudyField, modifier: Modifier = Modifier) {
    val backStack = LocalBackStack.current
    if (field.classroomSchedule) ClassroomDetailValue(field.value, modifier, field.originalValue ?: field.value, "安排", schedule = true)
    else if (field.label in classroomFieldLabels) ClassroomDetailValue(field.value, modifier, field.originalValue ?: field.value, field.label,
        applyReplacements = field.applyReplacements)
    else if (field.teacherQuery == null) SelectionContainer(modifier) {
        Text(if (field.label == "课程名称") courseNameText(field.value) else field.value, style = MaterialTheme.typography.bodyLarge)
    }
    else FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        field.teacherQuery.split(Regex("[、,，;；/\\n]+" )).map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { name ->
            Row(Modifier.clickable(role = Role.Button,
                onClick = { backStack.add(TeacherSearchRoute(name)) }),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


@Composable
internal fun CompactStudySectionContent(section: StudySection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(section.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                section.fields.forEach { field ->
                    if (field.value.length > 45 || LocalDensity.current.fontScale > 1.3f ||
                        (field.label.length > 4 && LocalDensity.current.fontScale > 1.2f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (field.courseLabel) courseNameText(field.label) else field.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StudyFieldValue(field)
                        }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (field.courseLabel) courseNameText(field.label) else field.label, Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        StudyFieldValue(field, Modifier.weight(1.6f))
                    }
                }
            }
        }
    }
}
