package com.chow.pomegranate.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.network.*
import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

@Serializable data object ConnectionSettingsRoute : NavKey

@KoinViewModel
class ConnectionSettingsViewModel(private val vpn: WebVpnSession, private val session: SessionManager,
    private val demo: DemoModeRepository) : ViewModel() {
    val state = vpn.state
    val demoMode = demo.enabled
    val busy: StateFlow<Boolean>
        field = MutableStateFlow(false)
    private val events = Channel<String>(Channel.BUFFERED)
    val messages = events.receiveAsFlow()
    init { viewModelScope.launch { vpn.initialize() } }
    private fun run(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { events.send("连接设置未完成，请重试。") }
            finally { busy.value = false }
        }
    }
    fun policy(value: ConnectionPolicy) = run {
        if (state.value.policy != value) {
            if (!demo.isEnabled()) session.logout()
            vpn.setPolicy(value)
        }
    }
    fun connect(account: String, password: String) = run {
        if (demo.isEnabled()) { events.send("演示模式不会连接真实服务。"); return@run }
        if (account.isBlank() || password.isBlank()) { events.send("请输入学号和统一身份认证密码。"); return@run }
        session.logout()
        vpn.begin(account, password)
    }
    fun otp(value: String) = run { vpn.submitOtp(value) }
    fun captcha(value: String) = run { vpn.submitCaptcha(value) }
    fun disconnect() = run {
        if (!demo.isEnabled()) session.logout()
        vpn.disconnect()
    }
}

@Composable
internal fun ConnectionModeCard(modifier: Modifier = Modifier) {
    val vpn = koinInject<WebVpnSession>()
    val state by vpn.state.collectAsState()
    val stack = LocalBackStack.current
    LaunchedEffect(vpn) { vpn.initialize() }
    Surface(onClick = { stack.add(ConnectionSettingsRoute) }, modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("连接方式", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(state.policy.label + when {
                state.policy == ConnectionPolicy.Direct -> ""
                (state.viaProxy || state.policy == ConnectionPolicy.Proxy) && state.stage !in setOf(WebVpnStage.Connected, WebVpnStage.Restoring) -> " · 需连接"
                state.policy == ConnectionPolicy.Proxy -> ""
                state.viaProxy -> " · 代理"
                else -> " · 直连"
            }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun ConnectionSettingsSheet(viewModel: ConnectionSettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val demo by viewModel.demoMode.collectAsState()
    val stack = LocalBackStack.current
    // ModalBottomSheet 使用独立窗口，提示需在当前弹层中显示；沿用统一的安全间距。
    val snackbar = remember { SnackbarHostState() }
    val vpn = koinInject<WebVpnSession>()
    val cleanupScope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val username = androidx.compose.foundation.text.input.rememberTextFieldState()
    val password = remember { TextFieldState() }
    val code = remember { TextFieldState() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.account) { if (username.text.isEmpty() && state.account.isNotEmpty()) username.edit { replace(0, length, state.account) } }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.stage) {
        code.edit { replace(0, length, "") }
        if (state.stage in setOf(WebVpnStage.Otp, WebVpnStage.Captcha, WebVpnStage.Connected, WebVpnStage.Failed)) password.edit { replace(0, length, "") }
    }
    DisposableEffect(vpn) { onDispose { cleanupScope.launch { vpn.cancelChallenge() } } }
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .85f }
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(max = height).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("连接方式", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                if (busy) PomLoadingIndicator(size = InlineLoadingIndicatorSize)
            }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ConnectionPolicy.entries.forEach { policy ->
                    Surface(onClick = { viewModel.policy(policy) }, enabled = !busy,
                        modifier = Modifier.semantics { role = Role.RadioButton; selected = state.policy == policy }, shape = MaterialTheme.shapes.large,
                        color = if (state.policy == policy) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RadioButton(state.policy == policy, onClick = null)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(policy.label, style = MaterialTheme.typography.titleSmall)
                                Text(when (policy) {
                                    ConnectionPolicy.Automatic -> "优先直连，连接失败时使用 WebVPN"
                                    ConnectionPolicy.Direct -> "仅通过校园网或系统 VPN 访问"
                                    ConnectionPolicy.Proxy -> "始终通过 WebVPN 访问"
                                }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Text("更改连接方式后，需要重新登录教务系统和第二课堂。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.policy != ConnectionPolicy.Direct) {
                HorizontalDivider()
                Text("WebVPN", style = MaterialTheme.typography.titleMedium)
                if (demo == true) Text("演示模式不会连接真实服务。", style = MaterialTheme.typography.bodyMedium)
                when (state.stage) {
                    WebVpnStage.Connected -> {
                        Text("已连接 · ${state.account}", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = viewModel::disconnect, enabled = !busy) { Text("断开连接") }
                            Button(onClick = { stack.removeLastOrNull() }) { Text("完成") }
                        }
                    }
                    WebVpnStage.Otp, WebVpnStage.Captcha -> {
                        val otp = state.stage == WebVpnStage.Otp
                        Text(if (otp) "输入统一身份认证的六位动态验证码。" else "完成统一身份认证的图片验证。", style = MaterialTheme.typography.bodyMedium)
                        state.captcha?.let { bytes ->
                            val bitmap = remember(bytes) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
                            bitmap?.let { Image(it, "统一认证验证码", Modifier.height(56.dp).fillMaxWidth()) }
                        }
                        PomStateTextField(code, Modifier.fillMaxWidth(), label = if (otp) "动态验证码" else "图片验证码", enabled = !busy,
                            keyboardOptions = KeyboardOptions(keyboardType = if (otp) KeyboardType.Number else KeyboardType.Ascii))
                        Button(onClick = { if (otp) viewModel.otp(code.text.toString()) else viewModel.captcha(code.text.toString()) }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()) { Text("验证并连接") }
                        TextButton(onClick = viewModel::disconnect, enabled = !busy) { Text("重新连接") }
                    }
                    else -> {
                        PomStateTextField(username, Modifier.fillMaxWidth(), label = "学号", enabled = !busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        PomStateTextField(password, Modifier.fillMaxWidth(), label = "统一身份认证密码", secure = true, visible = visible,
                            enabled = !busy, trailingIcon = { VisibilityIconButton(visible, { visible = it }) })
                        Button(onClick = { viewModel.connect(username.text.toString().trim(), password.text.toString()) }, enabled = !busy && demo != true,
                            modifier = Modifier.fillMaxWidth()) {
                            if (busy) { PomLoadingIndicator(size = InlineLoadingIndicatorSize); Spacer(Modifier.width(8.dp)) }
                            Text(if (busy) "正在连接" else "连接 WebVPN")
                        }
                    }
                }
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
