package com.chow.pomegranate.ui.screen.login


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Login
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.InlineLoadingIndicatorSize
import com.chow.pomegranate.ui.component.PomLoadingIndicator
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.component.VisibilityIconButton
import com.chow.pomegranate.ui.input.PasswordOutputTransformation
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.shimmerSpec
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.app_poem
import com.chow.pomegranate.captcha
import com.chow.pomegranate.collapse
import com.chow.pomegranate.default_extracurricular_password
import com.chow.pomegranate.expand
import com.chow.pomegranate.extracurricular_password
import com.chow.pomegranate.forget_password
import com.chow.pomegranate.ic_logo
import com.chow.pomegranate.logging_in
import com.chow.pomegranate.login
import com.chow.pomegranate.logo
import com.chow.pomegranate.password
import com.chow.pomegranate.refresh_captcha
import com.chow.pomegranate.username
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler

/** 融合门户，用于重置密码。 */
private const val PASSWORD_RESET_URL = "https://imy.gdufe.edu.cn"

/**
 * 登录页导航入口。
 */
fun EntryProviderScope<NavKey>.loginEntry() {
    entry<LoginRoute> {
        LoginScreen()
    }
}

/**
 * 登录页路由。
 */
@Serializable
data object LoginRoute : NavKey

/**
 * 登录页。
 */
@Composable
private fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    LoginContent(
        viewModel = viewModel,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun LoginContentPreview() {
    PomegranateExpressiveTheme {
        LoginContent()
    }
}

/**
 * 登录页内容。
 */
@Composable
private fun LoginContent(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel? = null,
) {
    val backStack = LocalBackStack.current

    // 打开重置密码页面
    val uriHandler = LocalUriHandler.current

    // 高斯模糊
    val sky = rememberSky()

    // 输入框状态
    val username = rememberTextFieldState()
    val password = rememberTextFieldState()
    val extracurricularPassword = rememberTextFieldState()
    val captcha = rememberTextFieldState()

    // 密码可见性
    var passwordVisible by remember { mutableStateOf(false) }
    // 第二课堂密码可见性
    var extracurricularPasswordVisible by remember { mutableStateOf(false) }

    // 第二课堂密码输入框是否展开
    var expanded by remember { mutableStateOf(false) }

    // 展开图标旋转角度
    val rotateDegrees by animateFloatAsState(
        if (expanded) 180f else 0f,
    )

    // 登录状态
    val isLoggingIn = viewModel?.isLoggingIn?.collectAsState()?.value ?: false

    // 验证码图片
    val captchaFlow = viewModel?.captcha
    val captchaImage by (captchaFlow ?: MutableStateFlow<ByteArray?>(null)).collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLastOrNull()
                },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.sky(sky = sky),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                // 介绍
                PomIntroduction(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                // 学号输入框
                TextField(
                    state = username,
                    label = {
                        Text(
                            stringResource(Res.string.username),
                        )
                    },
                    isError = username.text.isNotBlank() && username.text.length != 11,
                    inputTransformation = InputTransformation.maxLength(11)
                        .byValue { _, proposed ->
                            // 只能输入数字
                            proposed.filter { it.isDigit() }
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
            }

            item {
                // 密码输入框
                TextField(
                    state = password,
                    label = {
                        Text(
                            stringResource(Res.string.password),
                        )
                    },
                    trailingIcon = {
                        VisibilityIconButton(
                            visible = passwordVisible,
                            onVisibleChange = { passwordVisible = it },
                        )
                    },
                    outputTransformation = PasswordOutputTransformation()
                        .takeIf { !passwordVisible },
                )
            }

            item {
                AnimatedVisibility(expanded) {
                    // 第二课堂密码输入框
                    TextField(
                        state = extracurricularPassword,
                        label = {
                            Text(
                                stringResource(Res.string.extracurricular_password),
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(Res.string.default_extracurricular_password),
                            )
                        },
                        trailingIcon = {
                            VisibilityIconButton(
                                visible = extracurricularPasswordVisible,
                                onVisibleChange = { extracurricularPasswordVisible = it },
                            )
                        },
                        outputTransformation = PasswordOutputTransformation()
                            .takeIf { !extracurricularPasswordVisible },
                    )
                }

                // 展开收起按钮
                IconButton(
                    onClick = {
                        expanded = !expanded
                    },
                ) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) {
                                Res.string.collapse
                            } else {
                                Res.string.expand
                            },
                        ),
                        modifier = Modifier.rotate(rotateDegrees),
                    )
                }
            }

            if (viewModel != null) {
                item {
                    // 验证码
                    CaptchaSetting(
                        image = captchaImage,
                        state = captcha,
                        onRefresh = viewModel::refreshCaptcha,
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    // 忘记密码：前往融合门户重置
                    Text(
                        stringResource(Res.string.forget_password),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { uriHandler.openUri(PASSWORD_RESET_URL) },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                // 登录按钮
                LoginButton(
                    onClick = {
                        viewModel?.login(
                            userId = username.text.toString(),
                            password = password.text.toString(),
                            secondClassroomPassword = extracurricularPassword.text.toString(),
                            captcha = captcha.text.toString(),
                            onSuccess = {
                                // 登录成功，返回上一页
                                backStack.removeLastOrNull()
                            },
                        )
                    },
                    isLoggingIn = isLoggingIn,
                )
            }
        }
    }
}

/**
 * 验证码设置项。
 *
 * @param image 验证码图片
 * @param state 用户手动输入的验证码，留空时由程序自动识别
 * @param onRefresh 刷新验证码
 */
@Composable
private fun CaptchaSetting(
    image: ByteArray?,
    state: TextFieldState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 验证码图片
        Surface(
            onClick = onRefresh,
            modifier = Modifier.size(width = 120.dp, height = 56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (image != null) {
                Image(
                    bitmap = remember(image) { image.decodeToImageBitmap() },
                    contentDescription = stringResource(Res.string.refresh_captcha),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // 手动填写验证码，留空则自动识别
        TextField(
            state = state,
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    stringResource(Res.string.captcha),
                )
            },
            inputTransformation = InputTransformation.maxLength(4),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
            ),
        )

        // 刷新按钮
        IconButton(
            onClick = onRefresh,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(Res.string.refresh_captcha),
            )
        }
    }
}

/**
 * 顶部导航。
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PomTopAppBar(
        title = {},
        modifier = modifier,
        navigationIcon = {
            // 返回按钮
            BackIconButton(
                onClick = onBack,
            )
        },
    )
}

/**
 * 介绍。
 */
@Composable
private fun PomIntroduction(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            vertical = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo
        Surface(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 2.dp,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primaryFixed,
            ),
        ) {
            // 用 Image 而不是 Icon：Icon 默认会用主题色把 logo 染成单色，会丢掉渐变
            Image(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = stringResource(Res.string.logo),
                modifier = Modifier.size(72.dp).shimmer(
                    customShimmer = rememberShimmer(
                        shimmerBounds = ShimmerBounds.Window,
                        theme = LocalShimmerTheme.current.copy(
                            animationSpec = infiniteRepeatable(
                                animation = shimmerSpec(
                                    durationMillis = 4_000,
                                    easing = LinearEasing,
                                    delayMillis = 1_500,
                                ),
                                repeatMode = RepeatMode.Restart,
                            ),
                            blendMode = BlendMode.Screen,
                            rotation = 45f,
                            shaderColors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            ),
                            shaderColorStops = listOf(
                                0.0f,
                                0.4f,
                                0.45f,
                                0.55f,
                                0.60f,
                                1.0f,
                            ),
                            shimmerWidth = 20.dp,
                        ),
                    ),
                ),
            )
        }

        // 摘抄
        Text(
            stringResource(Res.string.app_poem),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * 输入框。
 */
@Composable
private fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label:@Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    shape: Shape = MaterialTheme.shapes.large,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    ),
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction ?: KeyboardActionHandler{
            focusManager.clearFocus()
        },
        lineLimits = lineLimits,
        shape = shape,
        colors = colors,
    )
}

/**
 * 登录按钮。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginButton(
    onClick: () -> Unit,
    isLoggingIn: Boolean,
) {
    ToggleButton(
        checked = true,
        onCheckedChange = {
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoggingIn,
        colors = ToggleButtonDefaults.toggleButtonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoggingIn) {
            // 登录中动画
            PomLoadingIndicator(size = InlineLoadingIndicatorSize)

            Spacer(modifier = Modifier.width(8.dp))

            // 登录中
            Text(stringResource(Res.string.logging_in))
        } else {
            // 登录图标
            Icon(
                Icons.AutoMirrored.TwoTone.Login,
                contentDescription = stringResource(Res.string.login),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 登录
            Text(stringResource(Res.string.login))
        }
    }
}