package com.chow.pomegranate.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.BackIconButton
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
import com.chow.pomegranate.username
import kotlin.time.Duration.Companion.seconds

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
) {
    LoginContent(
        modifier = modifier
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
) {
    val backStack = LocalBackStack.current

    // 高斯模糊
    val sky = rememberSky()

    // 学号
    val username = rememberTextFieldState()
    // 密码
    val password = rememberTextFieldState()
    // 第二课堂密码
    val extracurricularPassword = rememberTextFieldState()

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

    var isLoggingIn by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggingIn) {
        if (isLoggingIn) {
            delay(4.seconds)
            isLoggingIn = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onBack = {
                    backStack.removeLast()
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

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    // 忘记密码
                    Text(
                        stringResource(Res.string.forget_password),
                        modifier = Modifier.align(Alignment.End),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                // 登录按钮
                LoginButton(
                    onClick = {
                        isLoggingIn = true
                    },
                    isLoggingIn = isLoggingIn,
                )
            }
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
            Icon(
                painterResource(Res.drawable.ic_logo),
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
            LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = LocalContentColor.current,
            )

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