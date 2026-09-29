package com.chow.pomegranate.ui.screen.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.chow.pomegranate.Res
import com.chow.pomegranate.account_management
import com.chow.pomegranate.account_operation_failed
import com.chow.pomegranate.add_account
import com.chow.pomegranate.cancel
import com.chow.pomegranate.confirm_delete_account
import com.chow.pomegranate.current_account
import com.chow.pomegranate.delete
import com.chow.pomegranate.login
import com.chow.pomegranate.no_accounts
import com.chow.pomegranate.session.Account
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.login.LoginRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.confirm_logout
import com.chow.pomegranate.logout

/**
 * 账号管理弹窗路由。
 */
@Serializable
data object AccountRoute : NavKey

/**
 * 注册账号管理弹窗。
 *
 * 与 OTP 编辑弹窗一致，通过 [DialogSceneStrategy] 渲染为弹窗场景，
 * 这样下层内容仍会透出，返回键也能正确关闭弹窗。
 */
fun EntryProviderScope<NavKey>.accountEntry() {
    entry<AccountRoute>(metadata = DialogSceneStrategy.dialog()) {
        AccountDialog()
    }
}

/**
 * 账号管理弹窗。
 */
@Composable
private fun AccountDialog(viewModel: AccountViewModel = koinViewModel()) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val operationFailedMessage = stringResource(Res.string.account_operation_failed)

    val sessionManager = koinInject<SessionManager>()
    val accounts by sessionManager.accounts.collectAsState()
    val currentUserId by sessionManager.userId.collectAsState()

    // 等待删除确认的账号
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    // 是否等待退出登录确认
    var pendingLogout by remember { mutableStateOf(false) }

    // 是否正在退出登录
    var isLoggingOut by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { backStack.removeLastOrNull() },
        title = {
            Text(stringResource(Res.string.account_management))
        },
        text = {
            if (accounts.isEmpty()) {
                // 暂无账号
                Text(stringResource(Res.string.no_accounts))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(accounts, key = Account::userId) { account ->
                        AccountItem(
                            account = account,
                            isCurrent = account.userId == currentUserId,
                            onClick = {
                                // 先关弹窗，切号交给 ViewModel 跑完
                                backStack.removeLastOrNull()
                                viewModel.switchAccount(account.userId) { failure ->
                                    failure?.let {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                it.message ?: operationFailedMessage,
                                            )
                                        }
                                    }
                                }
                            },
                            onDelete = { pendingDelete = account },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 关闭弹窗后进入登录页添加账号
                    backStack.removeLastOrNull()
                    backStack.add(LoginRoute)
                },
            ) {
                Text(stringResource(Res.string.add_account))
            }
        },
        dismissButton = {
            if (currentUserId != null) {
                // 已登录时提供退出登录
                TextButton(
                    onClick = { pendingLogout = true },
                    enabled = !isLoggingOut,
                ) {
                    Text(stringResource(Res.string.logout))
                }
            } else {
                TextButton(
                    onClick = { backStack.removeLastOrNull() },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        },
    )

    // 退出登录确认
    if (pendingLogout) {
        AlertDialog(
            onDismissRequest = { pendingLogout = false },
            text = {
                Text(stringResource(Res.string.confirm_logout))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLoggingOut = true
                        pendingLogout = false
                        backStack.removeLastOrNull()

                        // 登出必须跑在 ViewModel 里：弹窗已关闭，composition scope 会被取消
                        viewModel.logout { failure ->
                            isLoggingOut = false
                            failure?.let {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        it.message ?: operationFailedMessage,
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isLoggingOut,
                ) {
                    Text(stringResource(Res.string.logout))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingLogout = false },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            text = {
                Text(stringResource(Res.string.confirm_delete_account, account.userId))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val userId = account.userId
                        pendingDelete = null
                        viewModel.deleteAccount(userId) { failure ->
                            failure?.let {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        it.message ?: operationFailedMessage,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

/**
 * 账号项。
 */
@Composable
private fun AccountItem(
    account: Account,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        ListItem(
            headlineContent = {
                Text(account.userId)
            },
            supportingContent = {
                if (isCurrent) {
                    // 当前账号
                    Text(stringResource(Res.string.current_account))
                }
            },
            leadingContent = {
                if (isCurrent) {
                    Icon(
                        Icons.TwoTone.CheckCircle,
                        contentDescription = stringResource(Res.string.current_account),
                    )
                }
            },
            trailingContent = {
                IconButton(
                    onClick = onDelete,
                ) {
                    Icon(
                        Icons.TwoTone.Delete,
                        contentDescription = stringResource(Res.string.delete),
                    )
                }
            },
        )
    }
}
