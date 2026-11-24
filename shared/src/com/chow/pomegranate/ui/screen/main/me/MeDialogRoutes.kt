package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.LocalAppCoroutineScope
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.navigation.AlertDialogSceneStrategy
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data class ThemeColorRoute(val initialHex: String? = null) : NavKey
@Serializable data object AvatarActionsRoute : NavKey

fun EntryProviderScope<NavKey>.meDialogEntries() {
    entry<ThemeColorRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { route -> ThemeColorDestination(route) }
    entry<AvatarActionsRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { AvatarActionsDestination() }
}

@Composable
private fun ThemeColorDestination(route: ThemeColorRoute, viewModel: MeViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    var draft by rememberSaveable { mutableStateOf(route.initialHex) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 颜色草稿由目的地保存，确认后才持久化，返回/点击外部不会留下未保存的主题。
    ThemeColorSheet(selected = draft?.toColor(), saving = saving, onColorChange = { draft = it.toHexString() },
        onSave = {
            if (!saving) {
                saving = true
                viewModel.saveThemeColor(draft?.toColor()) { failure ->
                    saving = false
                    if (failure == null) { if (backStack.lastOrNull() == route) backStack.removeLastOrNull() }
                    else error = failure.message
                }
            }
        }, onReset = { draft = null })
    // 保存失败使用应用级反馈，不另开弹窗。
    val snackbar = LocalSnackbarHostState.current
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }
}

@Composable
private fun AvatarActionsDestination(viewModel: MeViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val avatar by viewModel.avatarUri.collectAsState()
    val snackbar = LocalSnackbarHostState.current
    val fallback = rememberCoroutineScope()
    val feedback = LocalAppCoroutineScope.current ?: fallback
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val save: (String?) -> Unit = { uri ->
        saving = true
        viewModel.saveAvatar(uri) { failure ->
            saving = false
            if (failure == null) {
                backStack.removeLastOrNull()
                feedback.launch { snackbar.showSnackbar(if (uri == null) "已恢复默认头像。" else "头像已更新。") }
            } else error = failure.message
        }
    }
    val picker = rememberAvatarPicker { result ->
        when (result) {
            is AvatarPickResult.Selected -> save(result.localUri)
            is AvatarPickResult.Failed -> error = result.message
            is AvatarPickResult.Unavailable -> error = result.message
        }
    }
    AlertDialog(onDismissRequest = { if (!saving) backStack.removeLastOrNull() }, title = { Text("头像") },
        text = { Column { Text("仅修改本机头像，不会同步到学校账号。"); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(onClick = picker, enabled = !saving) { Text("更换头像") } },
        dismissButton = { if (avatar != null) TextButton(onClick = { save(null) }, enabled = !saving) { Text("恢复默认头像") } })
}
