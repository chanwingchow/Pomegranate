package com.chow.pomegranate.ui.screen.otp

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import com.chow.pomegranate.ui.component.BackToTopButton
import com.chow.pomegranate.ui.component.LocalAppCoroutineScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.chow.pomegranate.tool.clipboard.writePlainText
import com.chow.pomegranate.clipboard_copy_failed
import com.chow.pomegranate.Res
import com.chow.pomegranate.add
import com.chow.pomegranate.cancel
import com.chow.pomegranate.data.local.OtpAccountEntity
import com.chow.pomegranate.delete
import com.chow.pomegranate.edit
import com.chow.pomegranate.no_otp_accounts
import com.chow.pomegranate.otp
import com.chow.pomegranate.otp_account_actions
import com.chow.pomegranate.otp_code_copied
import com.chow.pomegranate.otp_confirm_delete_all
import com.chow.pomegranate.otp_confirm_delete_selected
import com.chow.pomegranate.otp_copy_code
import com.chow.pomegranate.otp_delete_all_title
import com.chow.pomegranate.otp_delete_failed
import com.chow.pomegranate.otp_delete_irreversible
import com.chow.pomegranate.otp_delete_selected
import com.chow.pomegranate.otp_delete_selected_title
import com.chow.pomegranate.otp_delete_title
import com.chow.pomegranate.otp_deleted_count
import com.chow.pomegranate.otp_deleting
import com.chow.pomegranate.otp_deselect_all
import com.chow.pomegranate.otp_more_accounts
import com.chow.pomegranate.otp_remaining_seconds
import com.chow.pomegranate.otp_select_accounts
import com.chow.pomegranate.otp_select_all
import com.chow.pomegranate.otp_selected_count
import com.chow.pomegranate.save
import com.chow.pomegranate.tool.otp.TotpGenerator
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.rememberDynamicMaterialThemeState
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 注册 OTP 页面与编辑弹窗。 */
fun EntryProviderScope<NavKey>.otpEntry() {
    entry<OtpRoute> { OtpScreen() }
    entry<OtpDeleteRoute>(metadata = DialogSceneStrategy.dialog()) { OtpDeleteDialog(it) }
    entry<OtpEditorRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { route ->
        OtpEditorSheet(route.accountId)
    }
}

/** OTP 页路由。 */
@Serializable
data object OtpRoute : NavKey

/** OTP 新增或编辑弹窗路由；空 ID 表示新增。 */
@Serializable
data class OtpEditorRoute(val accountId: Long? = null) : NavKey

/** OTP 页。 */
@Composable
private fun OtpScreen(
    modifier: Modifier = Modifier,
    viewModel: OtpViewModel = koinViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    OtpContent(accounts, viewModel::deleteAccounts, modifier)
}

@Preview(name = "窄屏手机", widthDp = 320, heightDp = 640)
@Preview(name = "手机", widthDp = 411, heightDp = 800)
@Preview(name = "折叠屏", widthDp = 673, heightDp = 840)
@Preview(name = "平板横屏", widthDp = 1200, heightDp = 800)
@Composable
private fun OtpContentPreview() {
    OtpGridPreview()
}

@Preview(name = "两倍字体", widthDp = 320, heightDp = 800)
@Composable
private fun OtpLargeFontPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
        OtpGridPreview()
    }
}

@Preview(name = "深色编辑模式", widthDp = 411, heightDp = 800)
@Composable
private fun OtpEditingPreview() {
    OtpGridPreview(isDark = true, isSelecting = true)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtpGridPreview(isDark: Boolean = false, isSelecting: Boolean = false) {
    DynamicMaterialExpressiveTheme(
        state = rememberDynamicMaterialThemeState(primary = DEFAULT_PRIMARY_COLOR, isDark = isDark),
    ) {
        Surface {
            OtpAccountGrid(
                accounts = listOf(
                    OtpAccountEntity(1, "GitHub", "octocat@example.com", "JBSWY3DPEHPK3PXP"),
                    OtpAccountEntity(
                        2, "GDUFE", "202600000001", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                        digits = 8, periodSeconds = 60,
                    ),
                    OtpAccountEntity(
                        3, "名称很长的服务 · Pomegranate", "long.account.name@example.com", "JBSWY3DPEHPK3PXP",
                    ),
                ),
                epochSeconds = 1_700_000_006,
                isSelecting = isSelecting,
                onEdit = {},
                onDelete = {},
                onCopy = {},
                selectedIds = if (isSelecting) setOf(1L) else emptySet(),
                onToggleSelection = {},
            )
        }
    }
}

@Serializable
data class OtpDeleteRoute(val ids: List<Long>, val labels: List<String>, val all: Boolean = false) : NavKey

private fun deletionRoute(accounts: List<OtpAccountEntity>, all: Boolean = false) = OtpDeleteRoute(
    accounts.map { it.id }, accounts.map { "${it.issuer} · ${it.accountName}" }, all)

/** OTP 页内容；普通操作与多选删除分别呈现。 */
@Composable
private fun OtpContent(
    accounts: List<OtpAccountEntity>,
    onDelete: (List<Long>, (Result<Int>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val clipboard = LocalClipboard.current
    val copyFailedMessage = stringResource(Res.string.clipboard_copy_failed)
    val copiedMessage = stringResource(Res.string.otp_code_copied)
    val deleteFailedMessage = stringResource(Res.string.otp_delete_failed)
    val scope = rememberCoroutineScope()
    var isSelecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable(
        stateSaver = listSaver<Set<Long>, Long>(save = { it.toList() }, restore = { it.toSet() }),
    ) { mutableStateOf(emptySet<Long>()) }
    val handleBack = {
        if (isSelecting) {
            isSelecting = false
            selectedIds = emptySet()
        } else backStack.removeLastOrNull()
        Unit
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = isSelecting && backStack.lastOrNull() == OtpRoute,
        onBackCompleted = handleBack,
    )
    val gridState = rememberLazyGridState()
    val accountIds = remember(accounts) { accounts.map { it.id }.toSet() }
    val allSelected = accountIds.isNotEmpty() && selectedIds.containsAll(accountIds)
    LaunchedEffect(accountIds) {
        selectedIds = selectedIds.intersect(accountIds)
        if (accountIds.isEmpty() || selectedIds.isEmpty()) isSelecting = false
    }
    val epochSeconds by produceState(Clock.System.now().epochSeconds) {
        while (true) {
            value = Clock.System.now().epochSeconds
            delay(1_000 - Clock.System.now().toEpochMilliseconds() % 1_000)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = {
                    Text(
                        if (isSelecting) stringResource(Res.string.otp_selected_count, selectedIds.size)
                        else stringResource(Res.string.otp),
                    )
                },
                navigationIcon = {
                    BackIconButton(onClick = handleBack)
                },
                actions = {
                    if (isSelecting) {
                        TextButton(onClick = { selectedIds = if (allSelected) emptySet() else accountIds }) {
                            Text(stringResource(if (allSelected) Res.string.otp_deselect_all else Res.string.otp_select_all))
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                backStack.add(deletionRoute(accounts.filter { it.id in selectedIds }, all = allSelected))
                            },
                        ) {
                            Icon(
                                Icons.TwoTone.Delete,
                                contentDescription = stringResource(Res.string.otp_delete_selected),
                                tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        IconButton(onClick = { isSelecting = true }, enabled = accounts.isNotEmpty()) {
                            Icon(Icons.Rounded.Checklist, contentDescription = stringResource(Res.string.otp_select_accounts))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                BackToTopButton(gridState)
                if (!isSelecting) SmallFloatingActionButton(onClick = { backStack.add(OtpEditorRoute()) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add))
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            OtpAccountGrid(
                accounts = accounts,
                gridState = gridState,
                epochSeconds = epochSeconds,
                isSelecting = isSelecting,
                selectedIds = selectedIds,
                onToggleSelection = { account ->
                    selectedIds = if (account.id in selectedIds) selectedIds - account.id else selectedIds + account.id
                },
                onEdit = { backStack.add(OtpEditorRoute(it.id)) },
                onDelete = { backStack.add(deletionRoute(listOf(it))) },
                onCopy = { code ->
                    scope.launch {
                        val feedback = try {
                            clipboard.writePlainText(code)
                            copiedMessage
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { copyFailedMessage }
                        snackbarHostState.showSnackbar(feedback)
                    }
                },
                modifier = Modifier.widthIn(max = 1200.dp).fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            )
        }
    }

}

@Composable
private fun OtpDeleteDialog(route: OtpDeleteRoute, viewModel: OtpViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val snackbar = LocalSnackbarHostState.current
    val fallback = rememberCoroutineScope()
    val scope = LocalAppCoroutineScope.current ?: fallback
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!deleting) backStack.removeLastOrNull() },
        properties = DialogProperties(dismissOnBackPress = !deleting, dismissOnClickOutside = !deleting),
        icon = { Icon(Icons.TwoTone.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(when {
            route.all -> Res.string.otp_delete_all_title
            route.ids.size == 1 -> Res.string.otp_delete_title
            else -> Res.string.otp_delete_selected_title
        })) },
        text = {
            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (route.all) Res.string.otp_confirm_delete_all else Res.string.otp_confirm_delete_selected, route.ids.size))
                route.labels.take(3).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (route.ids.size > 3) Text(stringResource(Res.string.otp_more_accounts, route.ids.size - 3))
                Text(stringResource(Res.string.otp_delete_irreversible), color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !deleting, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), onClick = {
            deleting = true
            viewModel.deleteAccounts(route.ids) { result ->
                deleting = false
                result.onSuccess { count ->
                    backStack.removeLastOrNull()
                    scope.launch { snackbar.showSnackbar(getString(Res.string.otp_deleted_count, count)) }
                }.onFailure { error = it.message ?: "删除失败，请重试。" }
            }
        }) { Text(stringResource(if (deleting) Res.string.otp_deleting else Res.string.delete)) } },
        dismissButton = { TextButton(enabled = !deleting, onClick = { backStack.removeLastOrNull() }) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** 随窗口宽度和字体大小调整列数，为放大的文字保留空间。 */
@Composable
private fun OtpAccountGrid(
    accounts: List<OtpAccountEntity>,
    epochSeconds: Long,
    isSelecting: Boolean,
    selectedIds: Set<Long>,
    onToggleSelection: (OtpAccountEntity) -> Unit,
    onEdit: (OtpAccountEntity) -> Unit,
    onDelete: (OtpAccountEntity) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    if (accounts.isEmpty()) {
        com.chow.pomegranate.ui.component.EmptyContent(modifier, contentPadding, stringResource(Res.string.no_otp_accounts))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp * fontScale),
        state = gridState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(accounts, key = { it.id }) { account ->
            OtpCard(
                account = account,
                epochSeconds = epochSeconds,
                isSelecting = isSelecting,
                isSelected = account.id in selectedIds,
                onToggleSelection = { onToggleSelection(account) },
                modifier = Modifier.animateItem(),
                onEdit = { onEdit(account) },
                onDelete = { onDelete(account) },
                onCopy = onCopy,
            )
        }
    }
}

/** Expressive 紧凑卡片；多选与复制反馈不改变验证码本身。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtpCard(
    account: OtpAccountEntity,
    epochSeconds: Long,
    isSelecting: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val generator = remember(account.secret, account.digits, account.periodSeconds) {
        TotpGenerator(account.secret, account.digits, account.periodSeconds)
    }
    val code = generator.generate(epochSeconds)
    val remaining = generator.remainingSeconds(epochSeconds)
    var copiedCode by remember(account.id) { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val isCopied = copiedCode == code
    val copyLabel = stringResource(Res.string.otp_copy_code)
    val selected = isSelecting && isSelected
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "OTP selection color",
    )
    val codeContainer by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "OTP code color",
    )
    val codeColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "OTP code content",
    )
    val countdownDescription = stringResource(Res.string.otp_remaining_seconds, remaining)
    val countdownColor = if (remaining <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    LaunchedEffect(copiedCode) {
        if (copiedCode != null) { delay(2_000); copiedCode = null }
    }
    Card(
        onClick = {
            if (isSelecting) onToggleSelection() else {
                val currentCode = generator.generate()
                onCopy(currentCode)
                copiedCode = currentCode
            }
        },
        modifier = modifier.fillMaxWidth().animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()).semantics {
            if (isSelecting) {
                role = Role.Checkbox
                toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
            } else onClick(label = copyLabel, action = null)
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(0.5.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            val fontScale = LocalDensity.current.fontScale
            val stackCode = fontScale > 1.3f || maxWidth < 260.dp || (account.digits > 6 && maxWidth < 300.dp * fontScale)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isSelecting) {
                                Checkbox(isSelected, onCheckedChange = null, modifier = Modifier.size(24.dp).clearAndSetSemantics {})
                            } else {
                                Surface(
                                    modifier = Modifier.size(24.dp * fontScale.coerceIn(1f, 2f)),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Crossfade(targetState = isCopied, label = "OTP copied") { copied ->
                                            if (copied) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                                            else Text(account.issuer.trim().ifBlank { account.accountName }.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics {})
                                        }
                                    }
                                }
                            }
                            Text(account.issuer.ifBlank { account.accountName }, style = MaterialTheme.typography.titleMedium,
                                maxLines = if (stackCode) 2 else 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(account.accountName, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium, maxLines = if (stackCode) 2 else 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    if (!stackCode) {
                        Surface(shape = MaterialTheme.shapes.medium, color = codeContainer, contentColor = codeColor) {
                            OtpCode(code, Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    if (!isSelecting) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.MoreVert, stringResource(Res.string.otp_account_actions, account.issuer))
                            }
                            DropdownMenu(menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.edit)) },
                                    leadingIcon = { Icon(Icons.TwoTone.Edit, null) }, onClick = { menuExpanded = false; onEdit() })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.TwoTone.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuExpanded = false; onDelete() })
                            }
                        }
                    }
                }
                if (stackCode) {
                    Surface(shape = MaterialTheme.shapes.medium, color = codeContainer, contentColor = codeColor) {
                        OtpCode(code, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), allowWrap = true)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = countdownDescription },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OtpCountdownProgress(epochSeconds, account.periodSeconds, countdownColor, Modifier.weight(1f).height(2.dp))
                    Text("${remaining}s", color = if (remaining <= 5) countdownColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/** Material3 进度条按帧平滑递减；新周期重建动画状态，立即复位。 */
@Composable
private fun OtpCountdownProgress(
    epochSeconds: Long,
    periodSeconds: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    key(periodSeconds, epochSeconds / periodSeconds) {
        val remaining = periodSeconds - epochSeconds % periodSeconds
        val millisIntoSecond = remember(epochSeconds) {
            val now = Clock.System.now()
            // 固定时间的预览不跟随真实时钟。
            if (now.epochSeconds == epochSeconds) now.nanosecondsOfSecond / 1_000_000 else 0
        }
        var target by remember {
            mutableFloatStateOf((remaining - millisIntoSecond / 1_000f) / periodSeconds)
        }
        val progress by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 1_000 - millisIntoSecond, easing = LinearEasing),
            label = "OTP countdown",
        )
        LaunchedEffect(epochSeconds) {
            target = (remaining - 1).toFloat() / periodSeconds
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = color,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** 验证码使用主题字号；极窄布局允许按分组换行，不缩小文字。 */
@Composable
private fun OtpCode(
    code: String,
    modifier: Modifier = Modifier,
    allowWrap: Boolean = false,
) {
    Text(
        text = code.chunked((code.length + 1) / 2).joinToString(" "),
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Monospace,
        maxLines = if (allowWrap) 2 else 1,
        softWrap = allowWrap,
    )
}
