package com.chow.pomegranate.ui.screen.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.*
import com.chow.pomegranate.session.Account
import com.chow.pomegranate.session.LoginTarget
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.AlertDialogSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.repository.AccountNicknameRepository
import com.chow.pomegranate.repository.accountNicknameKey
import com.chow.pomegranate.ui.screen.login.LoginRoute
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object AccountRoute : NavKey
@Serializable data class AccountConfirmRoute(val userId: String? = null) : NavKey

fun EntryProviderScope<NavKey>.accountEntry() {
    entry<AccountRoute> { AccountScreen() }
    entry<AccountConfirmRoute>(metadata = AlertDialogSceneStrategy.alertDialog()) { AccountConfirmDialog(it) }
    entry<AccountNicknameRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { AccountNicknameSheet(it) }
}

@Composable
private fun AccountScreen(viewModel: AccountViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val session = koinInject<SessionManager>()
    val accounts by session.accounts.collectAsState()
    val nicknameRepository = koinInject<AccountNicknameRepository>()
    val nicknames by nicknameRepository.names.collectAsState(initial = emptyMap())
    val currentId by session.userId.collectAsState()
    val switchingId by viewModel.switchingUserId.collectAsState()
    val error by viewModel.switchError.collectAsState()
    val operating = switchingId != null
    val listState = rememberLazyListState()
    Scaffold(topBar = {
        PomTopAppBar(title = { Text(stringResource(Res.string.account_management)) },
            navigationIcon = { BackIconButton(onClick = { backStack.removeLastOrNull() }) },
            actions = { IconButton(onClick = { backStack.add(LoginRoute) }, enabled = !operating) {
                Icon(Icons.Rounded.Add, stringResource(Res.string.add_account))
            } })
    }, floatingActionButton = { BackToTopButton(listState) }, bottomBar = {
        if (currentId != null) Surface(color = MaterialTheme.colorScheme.surface) {
            TextButton(onClick = { backStack.add(AccountConfirmRoute()) }, enabled = !operating,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.logout))
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (accounts.isEmpty()) EmptyContent(contentPadding = padding, message = stringResource(Res.string.no_accounts))
            else LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), state = listState, contentPadding = padding + PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("已保存 ${accounts.size} 个账号", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                error?.let { failure -> item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                        Text(failure.message ?: stringResource(Res.string.account_operation_failed), Modifier.fillMaxWidth().padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                } }
                itemsIndexed(accounts, key = { _, account -> account.userId }) { index, account ->
                    SavedAccountCard(account, index, account.userId == currentId, switchingId == account.userId, operating,
                        nickname = nicknames[accountNicknameKey(account.userId, account.isDemo)],
                        modifier = Modifier.animateItem(), onSwitch = { viewModel.switchAccount(account.userId) },
                        onEditNickname = { backStack.add(AccountNicknameRoute(account.userId, account.isDemo,
                            nicknames[accountNicknameKey(account.userId, account.isDemo)].orEmpty())) },
                        onDelete = { backStack.add(AccountConfirmRoute(account.userId)) })
                }
            }
        }
    }
}

@Composable
private fun SavedAccountCard(account: Account, index: Int, current: Boolean, switching: Boolean, operating: Boolean,
    nickname: String?, modifier: Modifier = Modifier, onSwitch: () -> Unit, onEditNickname: () -> Unit, onDelete: () -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    val services = when (account.target) {
        LoginTarget.Both -> "教务系统 · 第二课堂"
        LoginTarget.Academic -> "教务系统"
        LoginTarget.SecondClassroom -> "第二课堂"
    }
    val information: @Composable () -> Unit = {
        Text(nickname ?: account.userId, style = MaterialTheme.typography.titleMedium)
        if (nickname != null) Text(account.userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (account.isDemo) "演示账号 · $services" else services, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (current) Text(stringResource(Res.string.current_account), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
    ExpressiveActionCard(onClick = { if (!current && !operating) onSwitch() }, modifier = modifier, selected = current) { pressed ->
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExpressiveIconBadge(if (current) Icons.Rounded.CheckCircle else Icons.Rounded.PersonOutline, index, pressed, Modifier.size(36.dp), iconSize = 20.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { if (!largeText) information() }
                if (switching) PomLoadingIndicator(size = InlineLoadingIndicatorSize)
                IconButton(onClick = onEditNickname, enabled = !operating) { Icon(Icons.Rounded.Edit, "修改 ${account.userId} 的昵称") }
                IconButton(onClick = onDelete, enabled = !operating) { Icon(Icons.Outlined.DeleteOutline, "删除账号 ${account.userId}") }
            }
            if (largeText) information()
        }
    }
}

@Composable
private fun AccountConfirmDialog(route: AccountConfirmRoute, viewModel: AccountViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    var operating by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarHostState.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!operating) backStack.removeLastOrNull() },
        properties = DialogProperties(dismissOnBackPress = !operating, dismissOnClickOutside = !operating),
        title = { Text(if (route.userId == null) "退出登录" else "删除账号") },
        text = { Column {
            Text(if (route.userId == null) stringResource(Res.string.confirm_logout)
                else stringResource(Res.string.confirm_delete_account, route.userId))
        } },
        confirmButton = { TextButton(enabled = !operating, onClick = {
            operating = true
            val onResult: (Throwable?) -> Unit = { failure ->
                operating = false
                backStack.removeLastOrNull()
                if (failure != null) scope.launch {
                    val action = if (route.userId == null) "退出失败" else "删除失败"
                    snackbar.showSnackbar("$action：${failure.message ?: "请重试。"}")
                }
            }
            if (route.userId == null) viewModel.logout(onResult) else viewModel.deleteAccount(route.userId, onResult)
        }) { Text(if (operating) "处理中" else if (route.userId == null) "退出登录" else "删除") } },
        dismissButton = { TextButton(enabled = !operating, onClick = { backStack.removeLastOrNull() }) { Text("取消") } })
}
