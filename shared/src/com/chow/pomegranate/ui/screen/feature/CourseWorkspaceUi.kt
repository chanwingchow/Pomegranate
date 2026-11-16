package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable data class CourseBasketRoute(val drop: Boolean, val ownerKey: String) : NavKey

internal fun StudyDetailRoute.cartItem(): CourseCartItem? = action?.let {
    CourseCartItem(it.id, it.courseCode.ifBlank { it.id }, it.name, it.teacher, it.credits, it.category, this)
}

@Composable
internal fun courseActionColors(drop: Boolean): ButtonColors = if (drop)
    ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
else ButtonDefaults.filledTonalButtonColors()

internal fun MutableList<NavKey>.finishCourseOperation() {
    if (lastOrNull() !is CourseOperationRoute) return
    removeAt(lastIndex)
    if (lastOrNull().studyDetailRoute() != null) removeAt(lastIndex)
}

internal fun performCourseAction(detail: StudyDetailRoute, workspace: CourseSelectionWorkspace, backStack: MutableList<NavKey>,
    scope: CoroutineScope, snackbar: SnackbarHostState) {
    val item = detail.cartItem() ?: return
    if (detail.action?.ownerKey != workspace.state.value.key || workspace.state.value.key.isEmpty()) {
        scope.launch { snackbar.showSnackbar("账号或轮次已变化，请重新打开课程详情。") }
        return
    }
    if (detail.action.drop) backStack.add(CourseOperationRoute(CourseOperation.Drop, item.id, item.name, item.category,
        detail.summary, listOf(item), workspace.state.value.key))
    else scope.launch {
        try {
            val prepared = if (workspace.state.value.preferenceMode) item.copy(priority = workspace.suggestedPriority(item)) else item
            val report = workspace.submit(listOf(prepared), false) ?: return@launch
            if (report.successCount > 0 && backStack.lastOrNull().studyDetailRoute()?.id == detail.id) backStack.removeLastOrNull()
            if (report.successCount == 0) workspace.showError(report.results.firstOrNull()?.message)
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(report.message)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            workspace.showError(error.message ?: "提交失败，请重试。")
            snackbar.showSnackbar(error.message ?: "提交失败，请重试。")
        }
    }
}

@Composable
internal fun StudyCourseActionButton(detail: StudyDetailRoute, modifier: Modifier = Modifier) {
    val action = detail.action ?: return
    val workspace = koinInject<CourseSelectionWorkspace>()
    val work by workspace.state.collectAsState()
    val stack = LocalBackStack.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!action.drop && work.preferenceMode) detail.cartItem()?.let { CoursePrioritySelector(workspace, it) }
        work.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { performCourseAction(detail, workspace, stack, scope, snackbar) },
            modifier = Modifier.fillMaxWidth(), enabled = !work.running && !work.expired && work.key.isNotEmpty() && action.ownerKey == work.key,
            colors = if (action.drop) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                else ButtonDefaults.buttonColors()) {
            if (work.running) { PomLoadingIndicator(size = InlineLoadingIndicatorSize); Spacer(Modifier.width(8.dp)) }
            Text(if (action.drop) "退课" else if (work.preferenceMode) "提交志愿" else "选课")
        }
    }
}

@Composable
internal fun CoursePrioritySelector(workspace: CourseSelectionWorkspace, item: CourseCartItem) {
    val work by workspace.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val priority = workspace.suggestedPriority(item)
    Box {
        AssistChip(onClick = { expanded = true }, enabled = !work.running && !work.expired,
            label = { Text(priority?.let { "第${"一二三"[it - 1]}志愿" } ?: "设置志愿") },
            trailingIcon = { Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp)) })
        DropdownMenu(expanded, { expanded = false }) {
            (1..3).forEach { p -> DropdownMenuItem(text = { Text("第${"一二三"[p - 1]}志愿") }, onClick = { workspace.setPriority(item.id, p); expanded = false }) }
        }
    }
}

internal fun submitCourseBasket(workspace: CourseSelectionWorkspace, items: List<CourseCartItem>, scope: CoroutineScope, snackbar: SnackbarHostState) {
    scope.launch {
        try {
            val report = workspace.submit(items.map { if (workspace.state.value.preferenceMode) it.copy(priority = workspace.suggestedPriority(it)) else it }, false)
            report?.let { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it.message) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { workspace.showError(error.message ?: "提交失败，请重试。"); snackbar.showSnackbar(error.message ?: "提交失败，请重试。") }
    }
}

@Composable
internal fun CourseBasketSheet(route: CourseBasketRoute) {
    val workspace = koinInject<CourseSelectionWorkspace>()
    val work by workspace.state.collectAsState()
    val stack = LocalBackStack.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val items = if (route.drop) work.pendingDrop else work.pendingSelect
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .82f }
    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (route.drop) "已选退课项目" else "设置选课志愿", style = MaterialTheme.typography.titleLarge)
        if (route.ownerKey != work.key) { Text("账号或选课轮次已切换，请返回重新打开清单。"); return@Column }
        Text(if (work.preferenceMode && !route.drop) "按课程设置教学班的志愿顺序；提交成功不代表最终中签。" else "${items.size} 门课程 · ${credit(items.sumOf { it.credits })} 学分",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(courseNameText(item.name), style = MaterialTheme.typography.titleSmall)
                                Text("${item.teacher} · ${credit(item.credits)} 学分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { workspace.removePending(item.id, route.drop) }, enabled = !work.running) { Icon(Icons.Rounded.RemoveCircleOutline, "移出 ${item.name}") }
                        }
                        if (!route.drop && work.preferenceMode) CoursePrioritySelector(workspace, item)
                    }
                }
            }
            work.report?.let { report ->
                item { Text(report.message, style = MaterialTheme.typography.titleSmall) }
                items(report.results) { result ->
                    Text("${if (result.success) "✓" else "未成功"} ${result.item.name}：${result.message}", style = MaterialTheme.typography.bodyMedium,
                        color = if (result.success) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                }
            }
        }
        work.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        if (work.running) { Text("正在提交 ${work.completed} / ${work.total}"); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { workspace.clearPending(route.drop) }, enabled = items.isNotEmpty() && !work.running) { Text("清空清单") }
            Button(onClick = {
                if (route.drop) stack.add(CourseOperationRoute(CourseOperation.Drop, "batch", "${items.size} 门课程", items = items, ownerKey = work.key))
                else submitCourseBasket(workspace, items, scope, snackbar)
            }, enabled = items.isNotEmpty() && !work.running && !work.expired, modifier = Modifier.weight(1f),
                colors = if (route.drop) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) else ButtonDefaults.buttonColors()) {
                Text(if (route.drop) "退课（${items.size}）" else if (work.preferenceMode) "提交志愿" else "选课（${items.size}）")
            }
        }
    }
}


@Composable
internal fun FavoriteScopeHeader(workspace: CourseSelectionWorkspace) {
    val work by workspace.state.collectAsState()
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val expiredCount = (if (work.expired) work.favorites.size else 0) + work.archived.count { it.scope.isExpired() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(work.scope?.let { "${it.semester} · ${if (work.expired) "本轮已结束" else "当前轮次收藏"}" }.orEmpty(), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (expiredCount > 0) TextButton(enabled = !work.running, onClick = { scope.launch {
            try { val count = workspace.clearExpiredFavorites(); snackbar.showSnackbar("已清理 $count 条过期收藏") }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { snackbar.showSnackbar(error.message ?: "清理失败，请重试。") }
        } }) { Text("清理已过期（$expiredCount）", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun ArchivedFavoriteCard(archived: ArchivedCourseFavorite) {
    val stack = LocalBackStack.current
    val scope = archived.scope
    val course = archived.favorite.course
    ExpressiveActionCard(onClick = {
        val detail = course.record(com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory.valueOf(archived.favorite.category)).detail
        stack.openAdaptiveStudyDetail(detail.copy(id = "history:${scope.key}:${course.id}",
            summary = "${scope.semester} · ${scope.roundName} · 仅供查看", action = null,
            sections = detail.sections.map { section -> section.copy(fields = section.fields.map {
                if (it.label == "选课状态") it.copy(value = "历史记录，仅供参考") else it
            }) }))
    }) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(courseNameText(course.name), style = MaterialTheme.typography.titleMedium)
            Text("${course.teacher} · ${scope.semester}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${if (scope.isExpired()) "已过期" else "非当前轮次"} · ${scope.roundName}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
