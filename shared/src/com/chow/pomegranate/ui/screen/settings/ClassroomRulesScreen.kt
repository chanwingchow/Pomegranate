package com.chow.pomegranate.ui.screen.settings

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

@Serializable data object ClassroomRulesRoute : NavKey
@Serializable data class ClassroomRuleEditorRoute(val rule: ClassroomReplacementRule? = null,
    val target: NameReplacementTarget = rule?.target ?: NameReplacementTarget.Classroom) : NavKey
@Serializable data class ClassroomRuleDeleteRoute(val ids: List<String>) : NavKey
@Serializable data class ClassroomBuiltinRulesRoute(val target: NameReplacementTarget = NameReplacementTarget.Classroom) : NavKey

@KoinViewModel
class ClassroomRulesViewModel(private val repository: ClassroomReplacementRepository) : ViewModel() {
    val rules = repository.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val busy: StateFlow<Boolean>
        field = MutableStateFlow(false)
    private val events = Channel<String>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()

    private fun execute(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { events.send(if (failure is IllegalArgumentException) failure.message ?: "请检查填写内容。" else "保存失败，请重试。") }
            finally { busy.value = false }
        }
    }
    fun save(id: String?, source: String, replacement: String, target: NameReplacementTarget, onSaved: () -> Unit) = execute {
        repository.save(id, source, replacement, target); onSaved()
    }
    fun enabled(id: String, enabled: Boolean) = execute { repository.enabled(id, enabled) }
    fun enabled(ids: Set<String>, enabled: Boolean) = execute { repository.enabled(ids, enabled) }
    fun builtinEnabled(target: NameReplacementTarget, enabled: Boolean) = execute {
        repository.enabled(builtinNameRules.filter { it.target == target }.map { it.id }.toSet(), enabled)
    }
    fun delete(ids: List<String>, onDeleted: () -> Unit) = execute { repository.delete(ids.toSet()); onDeleted() }
}

@Composable
internal fun ClassroomRulesScreen(viewModel: ClassroomRulesViewModel = koinViewModel()) {
    val rules by viewModel.rules.collectAsState()
    var targetIndex by rememberSaveable { mutableIntStateOf(0) }
    val target = NameReplacementTarget.entries[targetIndex]
    val builtins = remember(rules, target) { rules.filter { it.isBuiltin && it.target == target } }
    val builtinGroups = remember(builtins) { builtins.groupBy { it.campus.orEmpty() to it.replacement }.values.toList() }
    val customRules = remember(rules, target) { rules.filter { !it.isBuiltin && it.target == target } }
    val busy by viewModel.busy.collectAsState()
    val stack = LocalBackStack.current
    val list = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    val add = { stack.add(ClassroomRuleEditorRoute(target = target)); Unit }
    var isSelecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() }),
    ) { mutableStateOf(emptySet<String>()) }
    val ruleIds = remember(customRules) { customRules.map { it.id }.toSet() }
    val allSelected = ruleIds.isNotEmpty() && selectedIds.containsAll(ruleIds)
    val handleBack = {
        if (isSelecting) { isSelecting = false; selectedIds = emptySet() }
        else stack.removeLastOrNull()
        Unit
    }
    fun toggle(id: String) { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }
    LaunchedEffect(ruleIds) {
        val previous = selectedIds
        selectedIds = previous.intersect(ruleIds)
        if (ruleIds.isEmpty() || previous.isNotEmpty() && selectedIds.isEmpty()) isSelecting = false
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = isSelecting && stack.lastOrNull() == ClassroomRulesRoute,
        onBackCompleted = handleBack,
    )
    Scaffold(topBar = {
        StudyTopBar {
        PomTopAppBar(title = { Text(if (isSelecting) "已选 ${selectedIds.size} 项" else "名称替换") }, shadowElevation = 0.dp,
            navigationIcon = { BackIconButton(onClick = handleBack) },
            actions = {
                if (isSelecting) {
                    TextButton(onClick = { selectedIds = if (allSelected) emptySet() else ruleIds }) {
                        Text(if (allSelected) "取消全选" else "全选")
                    }
                    IconButton(enabled = selectedIds.isNotEmpty() && !busy,
                        onClick = { stack.add(ClassroomRuleDeleteRoute(customRules.filter { it.id in selectedIds }.map { it.id })) }) {
                        Icon(Icons.Rounded.Delete, "删除选中规则",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    IconButton(onClick = { isSelecting = true }, enabled = customRules.isNotEmpty()) { Icon(Icons.Rounded.Checklist, "多选规则") }
                    IconButton(onClick = add) { Icon(Icons.Rounded.Add, "添加替换规则") }
                }
            })
        StudyTabs(NameReplacementTarget.entries.map { it.label }, targetIndex) {
            targetIndex = it; isSelecting = false; selectedIds = emptySet()
        }
        }
    }, floatingActionButton = { BackToTopButton(list) }) { padding ->
        // Keep the scrolling viewport behind the navigation bar; only the last item needs its inset.
        Box(Modifier.fillMaxSize().padding(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
        ).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), state = list,
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp,
                    bottom = 20.dp + padding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item(key = "builtins") {
                    Surface(onClick = { stack.add(ClassroomBuiltinRulesRoute(target)) }, enabled = !isSelecting,
                        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("内置规则", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text("${builtinGroups.count { group -> group.any { it.enabled } }}/${builtinGroups.size}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Rounded.ChevronRight, "查看内置规则", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(builtins.any { it.enabled }, { viewModel.builtinEnabled(target, it) }, enabled = !busy && !isSelecting)
                        }
                    }
                }
                if (customRules.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("暂无自定义规则", style = MaterialTheme.typography.titleMedium)
                        FilledTonalButton(onClick = add) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("添加规则") }
                    }
                }
                items(customRules, key = { it.id }) { rule ->
                    Surface(shape = MaterialTheme.shapes.large,
                        color = if (isSelecting && rule.id in selectedIds) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.clip(MaterialTheme.shapes.large).combinedClickable(
                            onClick = { if (isSelecting) toggle(rule.id) else stack.add(ClassroomRuleEditorRoute(rule)) },
                            onLongClickLabel = "选择规则",
                            onLongClick = { isSelecting = true; toggle(rule.id) },
                        ).semantics { if (isSelecting) { role = Role.Checkbox; selected = rule.id in selectedIds } }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(rule.source, style = MaterialTheme.typography.titleMedium)
                                Text("替换为 ${rule.replacement}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelecting) Checkbox(checked = rule.id in selectedIds, onCheckedChange = null)
                            else {
                                Icon(Icons.Rounded.Edit, "编辑规则", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Switch(rule.enabled, { viewModel.enabled(rule.id, it) }, enabled = !busy)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ClassroomRuleEditorSheet(route: ClassroomRuleEditorRoute, viewModel: ClassroomRulesViewModel = koinViewModel()) {
    val stack = LocalBackStack.current
    val busy by viewModel.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    var source by rememberSaveable { mutableStateOf(route.rule?.source.orEmpty()) }
    var target by rememberSaveable { mutableStateOf(route.rule?.replacement.orEmpty()) }
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .85f }
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(if (route.rule == null) "添加替换规则" else "编辑替换规则", style = MaterialTheme.typography.titleLarge)
            PomTextField(source, { source = it }, Modifier.fillMaxWidth(), label = "原文字",
                placeholder = if (route.target == NameReplacementTarget.Course) "例如：高等数学" else "例如：教学楼 C", enabled = !busy)
            PomTextField(target, { target = it }, Modifier.fillMaxWidth(), label = "替换为",
                placeholder = if (route.target == NameReplacementTarget.Course) "例如：高数" else "例如：第三教学楼 C", enabled = !busy)
            Button(onClick = { viewModel.save(route.rule?.id, source, target, route.target) { stack.removeLastOrNull() } },
                enabled = !busy && source.isNotBlank() && target.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                if (busy) { PomLoadingIndicator(size = InlineLoadingIndicatorSize); Spacer(Modifier.width(8.dp)) }
                Text("保存")
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun ClassroomRuleDeleteDialog(route: ClassroomRuleDeleteRoute, viewModel: ClassroomRulesViewModel = koinViewModel()) {
    val stack = LocalBackStack.current
    val busy by viewModel.busy.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    AlertDialog(onDismissRequest = { if (!busy) stack.removeLastOrNull() }, title = { Text("删除 ${route.ids.size} 条规则？") },
        text = { Text("删除后立即生效。") },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.delete(route.ids) {
            stack.removeLastOrNull()
        } }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { stack.removeLastOrNull() }) { Text("取消") } })
}

@Composable
internal fun ClassroomBuiltinRulesSheet(route: ClassroomBuiltinRulesRoute, viewModel: ClassroomRulesViewModel = koinViewModel()) {
    val rules by viewModel.rules.collectAsState()
    val builtins = remember(rules, route.target) { rules.filter { it.isBuiltin && it.target == route.target } }
    val groups = remember(builtins) { builtins.groupBy { it.campus.orEmpty() to it.replacement }.values.toList() }
    val busy by viewModel.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .85f }
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (route.target == NameReplacementTarget.Course) "课程简称" else "教室简称", style = MaterialTheme.typography.titleLarge)
                    Text("已启用 ${groups.count { group -> group.any { it.enabled } }}/${groups.size}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(builtins.any { it.enabled }, { viewModel.builtinEnabled(route.target, it) }, enabled = !busy)
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(148.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                builtins.groupBy { it.campus.orEmpty() }.forEach { (campus, group) ->
                    if (campus.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { Text(campus, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary) }
                    items(group.groupBy { it.replacement }.values.toList(), key = { it.first().id }) { aliases ->
                        val active = aliases.any { it.enabled }
                        Surface(shape = MaterialTheme.shapes.large,
                            color = if (active) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(.5.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = .4f) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clip(MaterialTheme.shapes.large).toggleable(active, enabled = !busy, role = Role.Checkbox,
                                onValueChange = { viewModel.enabled(aliases.map { it.id }.toSet(), it) })) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(aliases.first().replacement, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(if (active) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                        Modifier.size(20.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                }
                                Text(aliases.map { it.source }.distinct().joinToString(" / "), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
