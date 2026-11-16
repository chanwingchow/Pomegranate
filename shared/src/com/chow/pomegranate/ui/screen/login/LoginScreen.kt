package com.chow.pomegranate.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.*
import com.chow.pomegranate.session.LoginTarget
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.popDestination
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object LoginRoute : NavKey
@Serializable data object SecondClassLoginRoute : NavKey
@Serializable data class LoginErrorRoute(val message: String, val detail: String) : NavKey
fun EntryProviderScope<NavKey>.loginEntry() {
    entry<LoginRoute> { LoginScreen() }
    entry<SecondClassLoginRoute> { LoginScreen(initialTarget = LoginTarget.SecondClassroom) }
    entry<LoginErrorRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { route ->
        val backStack = LocalBackStack.current
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(route.message, style = MaterialTheme.typography.titleLarge)
            SelectionContainer { Text(route.detail, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun LoginScreen(viewModel: LoginViewModel = koinViewModel(), initialTarget: LoginTarget = LoginTarget.Both) {
    val backStack = LocalBackStack.current
    val uriHandler = LocalUriHandler.current
    val snackbar = LocalSnackbarHostState.current
    val scope = LocalAppCoroutineScope.current ?: rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var focusedField by remember { mutableIntStateOf(-1) }
    val fieldRequesters = remember { List(4) { BringIntoViewRequester() } }
    fun fieldModifier(index: Int) = Modifier.fillMaxWidth().bringIntoViewRequester(fieldRequesters[index]).onFocusChanged {
        if (it.isFocused) focusedField = index else if (focusedField == index) focusedField = -1
    }
    val username = rememberTextFieldState()
    // 密码仅存在于当前页面内存，不写入导航参数或 SavedState。
    val password = remember { TextFieldState() }
    val secondPassword = remember { TextFieldState() }
    val captcha = rememberTextFieldState()
    var targetIndex by rememberSaveable { mutableIntStateOf(initialTarget.ordinal) }
    val target = if (targetIndex == LoginTarget.SecondClassroom.ordinal) LoginTarget.SecondClassroom else LoginTarget.Both
    val targetListState = rememberLazyListState()
    LaunchedEffect(target) { targetListState.animateScrollToItem(if (target == LoginTarget.SecondClassroom) 1 else 0) }
    var saveAccount by rememberSaveable { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var secondVisible by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    val accounts by viewModel.accounts.collectAsState()
    val hasSavedAccount = accounts.any { it.userId == username.text.toString() }
    val loggingIn by viewModel.isLoggingIn.collectAsState()
    val loadingCaptcha by viewModel.isLoadingCaptcha.collectAsState()
    val bytes by viewModel.captcha.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.errors.collectLatest { error ->
            val message = if (error.message == Res.string.captcha) "验证码加载失败" else getString(error.message)
            val detail = error.detail?.takeIf { it.isNotBlank() }
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = if (detail != null) "查看详情" else null,
                withDismissAction = true, duration = if (detail != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed && detail != null) backStack.add(LoginErrorRoute(message, detail))
        }
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    LaunchedEffect(target) { viewModel.selectTarget(target) }
    val submit = {
        focus.clearFocus()
        viewModel.login(username.text.toString(), password.text.toString(), secondPassword.text.toString(),
            captcha.text.toString(), target, saveAccount) { backStack.popDestination() }
    }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
        Column {
            PomTopAppBar(title = { Text(stringResource(Res.string.login)) },
                navigationIcon = { BackIconButton(onClick = { backStack.popDestination() }) },
                actions = {
                    if (target.academic) TextButton(onClick = {
                        runCatching { uriHandler.openUri("https://imy.gdufe.edu.cn") }
                            .onFailure { scope.launch { snackbar.showSnackbar("无法打开密码重置页面，请稍后重试。") } }
                    }) { Text(stringResource(Res.string.forget_password), style = MaterialTheme.typography.labelLarge) }
                })
            LazyRow(state = targetListState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(2) { index ->
                    val option = if (index == 0) LoginTarget.Both else LoginTarget.SecondClassroom
                    FilterChip(target == option, onClick = { if (!loggingIn) targetIndex = option.ordinal }, enabled = !loggingIn,
                        label = { Text(if (index == 0) "全部登录" else "仅第二课堂") },
                        leadingIcon = { if (target == option) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) })
                }
            }
        }
    }) { padding ->
        LaunchedEffect(focusedField, padding.calculateBottomPadding()) {
            val requester = fieldRequesters.getOrNull(focusedField) ?: return@LaunchedEffect
            withFrameNanos { }
            requester.bringIntoView()
        }
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 520.dp).fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box {
                        PomStateTextField(username, fieldModifier(0), label = stringResource(Res.string.username), placeholder = "输入学号",
                            inputTransformation = InputTransformation.maxLength(11).byValue { _, proposed -> proposed.filter(Char::isDigit) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), enabled = !loggingIn,
                            leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) }, trailingIcon = {
                                if (accounts.isNotEmpty()) IconButton(onClick = { accountMenu = true }, enabled = !loggingIn) {
                                    Icon(Icons.Rounded.ExpandMore, "选择已保存账号")
                                }
                            })
                        DropdownMenu(accountMenu, onDismissRequest = { accountMenu = false }) {
                            accounts.forEach { account -> DropdownMenuItem(text = { Text(account.userId) }, onClick = {
                                username.setTextAndPlaceCursorAtEnd(account.userId)
                                password.edit { replace(0, length, "") }
                                secondPassword.edit { replace(0, length, "") }
                                accountMenu = false
                            }) }
                        }
                    }
                }
                if (target.academic) {
                    item {
                        PomStateTextField(password, fieldModifier(1), label = "教务系统密码",
                            placeholder = if (hasSavedAccount) "留空使用已保存的密码" else "输入教务系统密码",
                            secure = true, visible = passwordVisible, enabled = !loggingIn,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            leadingIcon = { Icon(Icons.Rounded.LockOpen, null) },
                            trailingIcon = { VisibilityIconButton(passwordVisible, { passwordVisible = it }) })
                    }
                    item {
                        PomStateTextField(captcha, fieldModifier(2), label = "验证码", placeholder = "自动识别，可手动填写", enabled = !loggingIn,
                            inputTransformation = InputTransformation.maxLength(4),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii,
                                imeAction = if (target == LoginTarget.Both && expanded) ImeAction.Next else ImeAction.Done),
                            onKeyboardAction = if (target == LoginTarget.Both && expanded) null else KeyboardActionHandler { submit() },
                            leadingContent = {
                                Surface(onClick = viewModel::refreshCaptcha, enabled = !loadingCaptcha && !loggingIn,
                                    shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.padding(vertical = 4.dp).width(96.dp).fillMaxHeight()) {
                                    Box(contentAlignment = Alignment.Center) {
                                        when {
                                            loadingCaptcha -> PomLoadingIndicator(size = InlineLoadingIndicatorSize)
                                            bitmap != null -> Image(bitmap, "验证码图片，点击刷新", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                            else -> Text("重试", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            })
                        if (!loadingCaptcha && bytes != null && bitmap == null) Text("验证码无法显示，点击图片重试。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (target.secondClassroom) item {
                    if (target == LoginTarget.Both) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                        Text("设置第二课堂密码"); Spacer(Modifier.width(8.dp))
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    AnimatedVisibility(target == LoginTarget.SecondClassroom || expanded) {
                        PomStateTextField(secondPassword, fieldModifier(3), label = "第二课堂密码",
                            placeholder = if (hasSavedAccount) "留空使用已保存的密码" else "默认学号，可留空",
                            secure = true, visible = secondVisible, enabled = !loggingIn,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            onKeyboardAction = KeyboardActionHandler { submit() }, leadingIcon = { Icon(Icons.Rounded.LockOpen, null) },
                            trailingIcon = { VisibilityIconButton(secondVisible, { secondVisible = it }) })
                    }
                }
                item {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.toggleable(saveAccount, enabled = !loggingIn, role = Role.Checkbox, onValueChange = { saveAccount = it }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(saveAccount, onCheckedChange = null, enabled = !loggingIn)
                            Text("保存账号", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Button(onClick = submit, enabled = !loggingIn && (!target.academic || !loadingCaptcha), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        if (loggingIn) { PomLoadingIndicator(size = InlineLoadingIndicatorSize); Spacer(Modifier.width(8.dp)) }
                        Text(if (loggingIn) stringResource(Res.string.logging_in) else if (saveAccount) "登录并保存账号" else stringResource(Res.string.login))
                    }
                }
            }
        }
    }
}
