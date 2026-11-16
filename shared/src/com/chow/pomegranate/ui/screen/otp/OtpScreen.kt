package com.chow.pomegranate.ui.screen.otp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.chow.pomegranate.Res
import com.chow.pomegranate.add
import com.chow.pomegranate.cancel
import com.chow.pomegranate.confirm_delete_otp
import com.chow.pomegranate.data.local.OtpAccountEntity
import com.chow.pomegranate.delete
import com.chow.pomegranate.edit
import com.chow.pomegranate.no_otp_accounts
import com.chow.pomegranate.otp
import com.chow.pomegranate.otp_account
import com.chow.pomegranate.otp_digits
import com.chow.pomegranate.otp_issuer
import com.chow.pomegranate.otp_period
import com.chow.pomegranate.otp_secret
import com.chow.pomegranate.repository.OtpAccountRepository
import com.chow.pomegranate.save
import com.chow.pomegranate.tool.otp.TotpGenerator
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.ui.platform.LocalClipboardManager
import com.chow.pomegranate.otp_advanced
import com.chow.pomegranate.otp_import
import com.chow.pomegranate.otp_import_algorithm
import com.chow.pomegranate.otp_import_empty
import com.chow.pomegranate.tool.otp.parseOtpAuthUri
import com.chow.pomegranate.collapse
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import com.chow.pomegranate.expand
import androidx.compose.material.icons.twotone.QrCodeScanner
import com.chow.pomegranate.qr_scan
import com.chow.pomegranate.tool.qr.QrScannerDialog

/** 注册 OTP 页面与编辑弹窗。 */
fun EntryProviderScope<NavKey>.otpEntry() {
    entry<OtpRoute> { OtpScreen() }
    entry<OtpEditorRoute>(metadata = DialogSceneStrategy.dialog()) { route ->
        OtpEditorDialog(route.accountId)
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
    OtpContent(accounts, viewModel::delete, modifier)
}

@Preview
@Composable
private fun OtpContentPreview() {
    PomegranateExpressiveTheme {
        OtpContent(
            accounts = listOf(
                OtpAccountEntity(
                    id = 1,
                    issuer = "GitHub",
                    accountName = "octocat@example.com",
                    secret = "JBSWY3DPEHPK3PXP",
                ),
                OtpAccountEntity(
                    id = 2,
                    issuer = "GDUFE",
                    accountName = "202600000001",
                    secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                    digits = 8,
                    periodSeconds = 60,
                ),
            ),
            onDelete = { _, _ -> },
        )
    }
}

/** OTP 页内容。 */
@Composable
private fun OtpContent(
    accounts: List<OtpAccountEntity>,
    onDelete: (OtpAccountEntity, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var pendingDeletion by remember { mutableStateOf<OtpAccountEntity?>(null) }
    val epochSeconds by produceState(Clock.System.now().epochSeconds) {
        while (true) {
            value = Clock.System.now().epochSeconds
            delay(1_000)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = { Text(stringResource(Res.string.otp)) },
                navigationIcon = {
                    BackIconButton(onClick = { backStack.removeLastOrNull() })
                },
                actions = {
                    IconToggleButton(checked = isEditing, onCheckedChange = { isEditing = it }) {
                        Icon(Icons.TwoTone.Edit, contentDescription = stringResource(Res.string.edit))
                    }
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { backStack.add(OtpEditorRoute()) }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add))
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (accounts.isEmpty()) {
                item { Text(stringResource(Res.string.no_otp_accounts)) }
            }
            items(accounts, key = { it.id }) { account ->
                OtpCard(
                    account = account,
                    epochSeconds = epochSeconds,
                    isEditing = isEditing,
                    onEdit = { backStack.add(OtpEditorRoute(account.id)) },
                    onDelete = { pendingDeletion = account },
                )
            }
        }
    }

    pendingDeletion?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text(stringResource(Res.string.delete)) },
            text = { Text(stringResource(Res.string.confirm_delete_otp, account.accountName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeletion = null
                        onDelete(account) { result ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "已删除 " + account.accountName + "。" },
                                        onFailure = { it.message ?: "删除失败。" },
                                    ),
                                )
                            }
                        }
                    },
                ) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

/** OTP 卡片。 */
@Composable
private fun OtpCard(
    account: OtpAccountEntity,
    epochSeconds: Long,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val generator = remember(account.secret, account.digits, account.periodSeconds) {
        TotpGenerator(account.secret, account.digits, account.periodSeconds)
    }
    val remaining = generator.remainingSeconds(epochSeconds)
    Card(
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primaryFixed),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(account.issuer, style = MaterialTheme.typography.titleMedium)
            Text(account.accountName, style = MaterialTheme.typography.bodyMedium)
            Text(
                generator.generate(epochSeconds).chunked(3).joinToString(" "),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { remaining.toFloat() / account.periodSeconds },
                    modifier = Modifier.weight(1f),
                )
                Text(remaining.toString() + "s")
            }
            if (isEditing) {
                Row(modifier = Modifier.align(Alignment.End)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.TwoTone.Edit, contentDescription = stringResource(Res.string.edit))
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.TwoTone.Delete, contentDescription = stringResource(Res.string.delete))
                    }
                }
            }
        }
    }
}

/** OTP 新增或编辑弹窗。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtpEditorDialog(
    accountId: Long?,
    repository: OtpAccountRepository = koinInject(),
) {
    val backStack = LocalBackStack.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var issuer by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("6") }
    var period by remember { mutableStateOf("30") }
    var loaded by remember { mutableStateOf(accountId == null) }

    // 从剪贴板导入 otpauth:// 链接
    val clipboard = LocalClipboardManager.current

    // 高级选项（位数、周期）默认收起，绝大多数服务用 6 位 / 30 秒
    var advanced by remember { mutableStateOf(false) }

    // 展开箭头旋转角度，用动画过渡而不是直接换图标
    val advancedRotation by animateFloatAsState(if (advanced) 180f else 0f)

    // 导入提示，null 表示无提示
    var message by remember { mutableStateOf<String?>(null) }

    // 是否正在扫码
    var scanning by remember { mutableStateOf(false) }

    // 提示文案在这里取好，onClick 里不能再调用 composable
    val importEmptyMessage = stringResource(Res.string.otp_import_empty)
    val importAlgorithmMessage = stringResource(Res.string.otp_import_algorithm)

    /**
     * 解析 otpauth:// 内容并填入表单。
     *
     * 扫码与粘贴链接共用这段逻辑，返回值为提示信息，null 表示成功。
     */
    val applyOtpAuthText: (String) -> String? = { text ->
        val account = parseOtpAuthUri(text)

        when {
            account == null -> importEmptyMessage

            // 生成器只实现 SHA-1，其他算法会算出错误的验证码
            account.algorithm != "SHA1" -> importAlgorithmMessage

            else -> {
                issuer = account.issuer
                accountName = account.accountName
                secret = account.secret
                digits = account.digits.toString()
                period = account.periodSeconds.toString()
                null
            }
        }
    }

    LaunchedEffect(accountId) {
        if (accountId != null) {
            runCatching { repository.get(accountId) }
                .onSuccess { account ->
                    if (account == null) {
                        snackbarHostState.showSnackbar("找不到该 OTP 账户。")
                        backStack.removeLastOrNull()
                    } else {
                        issuer = account.issuer
                        accountName = account.accountName
                        secret = account.secret
                        digits = account.digits.toString()
                        period = account.periodSeconds.toString()
                        loaded = true
                    }
                }
                .onFailure {
                    snackbarHostState.showSnackbar(it.message ?: "加载失败。")
                    backStack.removeLastOrNull()
                }
        }
    }

    if (!loaded) return
    // 内容放进 Material3 弹窗的各个槽位，容器由 AlertDialog 自己提供
    AlertDialog(
        onDismissRequest = { backStack.removeLastOrNull() },
        title = {
            Text(
                if (accountId == null) {
                    stringResource(Res.string.add)
                } else {
                    stringResource(Res.string.edit)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 主流做法是扫二维码，一次填好全部字段
                TextButton(
                    onClick = {
                        message = null
                        scanning = true
                    },
                ) {
                    Icon(Icons.TwoTone.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.qr_scan))
                }

                // 拿到链接文本时也可以直接粘贴导入
                TextButton(
                    onClick = {
                        message = applyOtpAuthText(clipboard.getText()?.text.orEmpty())
                    },
                ) {
                    Icon(Icons.TwoTone.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_import))
                }

                // 导入提示
                message?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                EditorField(issuer, { issuer = it }, stringResource(Res.string.otp_issuer))
                EditorField(accountName, { accountName = it }, stringResource(Res.string.otp_account))
                EditorField(secret, { secret = it }, stringResource(Res.string.otp_secret))

                // 高级选项，默认收起
                TextButton(onClick = { advanced = !advanced }) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(
                            if (advanced) Res.string.collapse else Res.string.expand,
                        ),
                        modifier = Modifier.rotate(advancedRotation),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_advanced))
                }

                AnimatedVisibility(visible = advanced) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditorField(
                            digits,
                            { digits = it.filter(Char::isDigit) },
                            stringResource(Res.string.otp_digits),
                            Modifier.weight(1f),
                        )
                        EditorField(
                            period,
                            { period = it.filter(Char::isDigit) },
                            stringResource(Res.string.otp_period),
                            Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            repository.save(
                                OtpAccountEntity(
                                    id = accountId ?: 0,
                                    issuer = issuer,
                                    accountName = accountName,
                                    secret = secret,
                                    digits = digits.toIntOrNull() ?: 0,
                                    periodSeconds = period.toLongOrNull() ?: 0,
                                ),
                            )
                        }.onSuccess {
                            snackbarHostState.showSnackbar(
                                if (accountId == null) "OTP 账户已添加。" else "OTP 账户已更新。",
                            )
                            backStack.removeLastOrNull()
                        }.onFailure {
                            snackbarHostState.showSnackbar(it.message ?: "保存失败。")
                        }
                    }
                },
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = { backStack.removeLastOrNull() }) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )

    // 扫码界面，识别成功后填入表单
    if (scanning) {
        QrScannerDialog(
            onResult = { text ->
                scanning = false
                message = applyOtpAuthText(text)
            },
            onDismiss = { scanning = false },
        )
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}
