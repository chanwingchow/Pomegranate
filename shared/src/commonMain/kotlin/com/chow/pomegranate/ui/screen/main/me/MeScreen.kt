package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.twotone.ColorLens
import androidx.compose.material.icons.twotone.Face4
import androidx.compose.material.icons.twotone.School
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.chow.pomegranate.BuildConfig
import com.chow.pomegranate.Res
import com.chow.pomegranate.app_name
import com.chow.pomegranate.cancel
import com.chow.pomegranate.avatar
import com.chow.pomegranate.avatar_updated
import com.chow.pomegranate.avatar_restored
import com.chow.pomegranate.avatar_local_only
import com.chow.pomegranate.change_avatar
import com.chow.pomegranate.campus
import com.chow.pomegranate.campus_canton
import com.chow.pomegranate.favorite_color
import com.chow.pomegranate.gender
import com.chow.pomegranate.ic_logo
import com.chow.pomegranate.login
import com.chow.pomegranate.settings
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.account.AccountRoute
import com.chow.pomegranate.ui.screen.settings.SettingsRoute
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.wallpaper
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.chow.pomegranate.save
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.github.skydoves.colorpicker.compose.AlphaSlider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import com.chow.pomegranate.color_hex
import com.chow.pomegranate.color_hex_invalid
import com.chow.pomegranate.reset
import com.chow.pomegranate.restore_default_avatar
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

/** 十六进制颜色长度：6 位为 RGB，8 位为 ARGB。 */
private const val HEX_COLOR_LENGTH_RGB = 6
private const val HEX_COLOR_LENGTH_ARGB = 8

/**
 * 壁纸遮罩透明度。
 *
 * 越大壁纸越淡、文字越清楚，可按喜好微调。
 */
private const val WALLPAPER_SCRIM_ALPHA = 0.7f

/**
 * 我的页路由。
 */
@Serializable
data object MeRoute : NavKey

/**
 * 我的页。
 */
@Composable
fun MeScreen(
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
    viewModel: MeViewModel = koinViewModel(),
) {
    MeContent(
        toolbarExpanded = toolbarExpanded,
        onToolbarExpandedChange = onToolbarExpandedChange,
        viewModel = viewModel,
    )
}

@Preview
@Composable
private fun MeContentPreview() {
    PomegranateExpressiveTheme {
        MeContent(
            toolbarExpanded = true,
            onToolbarExpandedChange = {},
        )
    }
}

/**
 * 我的页内容。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MeContent(
    toolbarExpanded: Boolean,
    onToolbarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeViewModel? = null,
) {
    val backStack = LocalBackStack.current
    val layoutDirection = LocalLayoutDirection.current

    // 当前登录用户
    val userId = viewModel?.sessionManager?.userId?.collectAsState()?.value

    // 性别
    val gender = viewModel?.gender?.collectAsState()?.value ?: Gender.Default

    // 校区
    val campus = viewModel?.campus?.collectAsState()?.value ?: CampusOption.Default

    // 主题色，null 表示默认色
    val themeColor = viewModel?.themeColor?.collectAsState()?.value

    // 主题色选择弹窗
    var colorPickerVisible by remember { mutableStateOf(false) }

    // 每日壁纸
    val wallpaperUrl = viewModel?.wallpaperUrl?.collectAsState()?.value

    // 头像是应用私有文件的 URI，不是远端账户资料。
    val avatar = viewModel?.avatarUri?.collectAsState()?.value
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val avatarUpdatedMessage = stringResource(Res.string.avatar_updated)
    val avatarRestoredMessage = stringResource(Res.string.avatar_restored)
    val avatarLocalOnlyMessage = stringResource(Res.string.avatar_local_only)
    var avatarActionsVisible by remember { mutableStateOf(false) }
    val avatarPicker = rememberAvatarPicker { result ->
        when (result) {
            is AvatarPickResult.Selected -> {
                viewModel?.setLocalAvatar(result.localUri)
                scope.launch { snackbarHostState.showSnackbar(avatarUpdatedMessage) }
            }
            is AvatarPickResult.Failed -> scope.launch { snackbarHostState.showSnackbar(result.message) }
            is AvatarPickResult.Unavailable -> scope.launch { snackbarHostState.showSnackbar(result.message) }
        }
    }

    var expanded by remember { mutableStateOf(true) }
    val userCardHeight by animateDpAsState(
        if (expanded) 180.dp else 88.dp
    )

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        val paddingStart = innerPadding.calculateStartPadding(
            layoutDirection = layoutDirection,
        )
        val paddingTop = innerPadding.calculateTopPadding()
        val paddingEnd = innerPadding.calculateEndPadding(
            layoutDirection = layoutDirection,
        )
        val paddingBottom = innerPadding.calculateBottomPadding()

        Column(
            modifier = Modifier.padding(
                start = paddingStart,
                top = paddingTop,
                end = paddingEnd,
            ),
        ) {
            // 用户卡片
            UserCard(
                onClick = {
                    if (userId == null) {
                        // 未登录时直接进入登录页，不弹账号管理
                        backStack.add(LoginRoute)
                    } else {
                        // 已登录时打开账号管理
                        backStack.add(AccountRoute)
                    }
                },
                avatar = avatar,
                onAvatarClick = { avatarActionsVisible = true },
                background = wallpaperUrl,
                userId = userId,
                modifier = Modifier.padding(16.dp).height(userCardHeight),
            )

            LazyColumn(
                modifier = Modifier.weight(1f).floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = {
                        onToolbarExpandedChange(true)
                        expanded = true
                    },
                    onCollapse = {
                        onToolbarExpandedChange(false)
                        expanded = false
                    },
                ),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + paddingBottom + with(FloatingToolbarDefaults) {
                        // 悬浮工具栏高度
                        ContainerSize + ScreenOffset
                    },
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    TagGroup(
                        campus = campus,
                        onToggleCampus = { viewModel?.toggleCampus() },
                        gender = gender,
                        onToggleGender = { viewModel?.toggleGender() },
                        themeColor = themeColor,
                        onPickColor = { colorPickerVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    // 设置
                    OutlinedCard(
                        onClick = {
                            // 跳转到设置页
                            backStack.add(SettingsRoute)
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(Res.string.settings),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    if (colorPickerVisible) {
        // 主题色选择
        ThemeColorDialog(
            selected = themeColor,
            onColorChange = { viewModel?.setThemeColor(it) },
            onSave = { viewModel?.commitThemeColor() },
            onCancel = { viewModel?.cancelThemeColor() },
            onReset = { viewModel?.resetThemeColor() },
            onDismiss = { colorPickerVisible = false },
        )
    }

    if (avatarActionsVisible) {
        AlertDialog(
            onDismissRequest = { avatarActionsVisible = false },
            title = { Text(stringResource(Res.string.avatar)) },
            text = { Text(avatarLocalOnlyMessage) },
            confirmButton = {
                TextButton(onClick = {
                    avatarActionsVisible = false
                    avatarPicker()
                }) { Text(stringResource(Res.string.change_avatar)) }
            },
            dismissButton = {
                if (avatar != null) {
                    TextButton(onClick = {
                        viewModel.clearLocalAvatar()
                        avatarActionsVisible = false
                        scope.launch { snackbarHostState.showSnackbar(avatarRestoredMessage) }
                    }) { Text(stringResource(Res.string.restore_default_avatar)) }
                }
            },
        )
    }
}

/**
 * 主题色选择弹窗。
 *
 * 圆形色盘选色 + 透明度滑块，并实时展示当前颜色。
 */
@Composable
private fun ThemeColorDialog(
    selected: Color?,
    onColorChange: (Color) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val controller = rememberColorPickerController()

    // 当前颜色，含透明度
    var color by remember { mutableStateOf(selected ?: DEFAULT_PRIMARY_COLOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.favorite_color))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 圆形色盘
                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    controller = controller,
                    initialColor = color,
                    onColorChanged = { envelope ->
                        color = envelope.color.copy(alpha = color.alpha)

                        // 拖动时即时预览
                        onColorChange(color)
                    },
                )

                // 透明度
                AlphaSlider(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    controller = controller,
                    initialColor = color,
                    onColorChanged = { envelope ->
                        // 透明滑块给出的是带 alpha 的颜色
                        color = envelope.color
                        onColorChange(color)
                    },
                )

                // 选中颜色展示与十六进制输入
                SelectedColorPreview(
                    color = color,
                    onColorChange = { picked ->
                        color = picked
                        onColorChange(picked)
                    },
                )
            }
        },
        confirmButton = {
            Row {
                // 取消：放弃本次修改并关闭
                TextButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(Res.string.cancel))
                }

                // 保存：保留当前颜色并关闭
                TextButton(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(Res.string.save))
                }
            }
        },
        dismissButton = {
            // 重置：回到默认色，弹窗保持打开以便继续调整
            TextButton(
                onClick = {
                    color = DEFAULT_PRIMARY_COLOR
                    controller.selectByColor(DEFAULT_PRIMARY_COLOR, fromUser = false)
                    onReset()
                },
            ) {
                Text(stringResource(Res.string.reset))
            }
        },
    )
}

/**
 * 选中颜色展示。
 *
 * 左侧是颜色色块（棋盘衬底便于看出透明），右侧可直接输入十六进制颜色。
 *
 * @param color 当前颜色
 * @param onColorChange 输入合法时回调新颜色
 */
@Composable
private fun SelectedColorPreview(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 输入框内容，用户可直接编辑
    var input by remember { mutableStateOf(color.toHexString().uppercase()) }

    // 颜色由色盘或透明度滑块改变时才同步到输入框。
    // 若当前文本已经对应这个颜色，说明这次改变来自输入框本身，不能回写，
    // 否则用户每删到 6 位合法值就会被补全，永远删不干净。
    LaunchedEffect(color) {
        if (input.toColor() != color) {
            input = color.toHexString().uppercase()
        }
    }

    // 只有完整的 6 位或 8 位才算合法
    val isValid = input.length in setOf(HEX_COLOR_LENGTH_RGB, HEX_COLOR_LENGTH_ARGB) &&
        input.toColor() != null

    // 输入是否已获得焦点，用于判断编辑结束
    var isFocused by remember { mutableStateOf(false) }

    // 编辑结束后才应用输入的颜色。
    // 若每次按键都应用，删除过程中经过的 6 位合法值会被当成新颜色，导致颜色乱跳。
    val applyInput = {
        if (isValid) input.toColor()?.let(onColorChange)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 颜色色块
        Surface(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(40.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(color))
        }

        // 十六进制输入
        OutlinedTextField(
            value = input,
            onValueChange = { value ->
                // 只保留十六进制字符，最多 8 位，允许清空
                input = value
                    .filter { it.isLetterOrDigit() }
                    .take(HEX_COLOR_LENGTH_ARGB)
                    .uppercase()
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { state ->
                    // 离开输入框时应用
                    if (isFocused && !state.isFocused) applyInput()
                    isFocused = state.isFocused
                },
            label = {
                Text(stringResource(Res.string.color_hex))
            },
            singleLine = true,
            isError = !isValid,
            supportingText = if (isValid) {
                null
            } else {
                {
                    Text(stringResource(Res.string.color_hex_invalid))
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                // 按下完成键时应用
                onDone = { applyInput() },
            ),
        )
    }
}

/**
 * 用户卡片。
 */
@Composable
private fun UserCard(
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    avatar: Any?,
    background: Any?,
    userId: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        // 文字用 primary，与 onPrimary 遮罩互相对比，换任何主题色都能看清
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 1.dp,
    ) {
        Box {
            // 背景图
            AsyncImage(
                model = background,
                modifier = Modifier.matchParentSize(),
                contentDescription = stringResource(Res.string.wallpaper),
                contentScale = ContentScale.Crop,
            )

            // 遮罩：纯色，用 onPrimary。
            // 始终绘制，这样壁纸还没加载出来时卡片也是柔和的浅色，
            // 不会直接露出高饱和的卡片底色。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = WALLPAPER_SCRIM_ALPHA),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 旋转头像
                RotatingAvatar(
                    modifier = Modifier.size(56.dp),
                    onClick = onAvatarClick,
                ) {
                    if (avatar == null) {
                        // 默认头像（Image 不做主题染色，保留 logo 的渐变）
                        Image(
                            painter = painterResource(Res.drawable.ic_logo),
                            contentDescription = stringResource(Res.string.avatar),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        AsyncImage(
                            model = avatar,
                            contentDescription = stringResource(Res.string.avatar),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    // 昵称
                    Text(
                        stringResource(Res.string.app_name),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    // 学号
                    Text(
                        userId ?: stringResource(Res.string.login),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // 版本号
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(0.4f),
                contentColor = LocalContentColor.current.copy(0.6f),
            ) {
                Text(
                    BuildConfig.versionName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * 旋转头像。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RotatingAvatar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animationSpec = infiniteRepeatable<Float>(tween(4000, easing = LinearEasing))

    // 内旋转
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = animationSpec,
    )
    // 外旋转
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = animationSpec,
    )

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.rotate(outerRotation),
        shape = MaterialShapes.Cookie7Sided.toShape(),
    ) {
        Box(
            modifier = Modifier.rotate(innerRotation),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

/**
 * 标签组。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TagGroup(
    campus: CampusOption,
    onToggleCampus: () -> Unit,
    gender: Gender,
    onToggleGender: () -> Unit,
    themeColor: Color?,
    onPickColor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = listOf(
        MaterialShapes.Cookie4Sided.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        MaterialShapes.Clover8Leaf.toShape(),
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // 校区，点击切换
        Tag(
            onClick = onToggleCampus,
            icon = {
                Icon(
                    Icons.TwoTone.School,
                    contentDescription = stringResource(Res.string.campus),
                )
            },
            label = {
                Text(
                    stringResource(campus.label),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = shapes.random(),
        )

        // 性别，点击切换
        Tag(
            onClick = onToggleGender,
            icon = {
                Icon(
                    gender.icon,
                    contentDescription = stringResource(Res.string.gender),
                )
            },
            label = {
                Text(
                    stringResource(gender.label),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = shapes.random(),
        )

        // 喜欢的颜色，点击选择主题色
        Tag(
            onClick = onPickColor,
            icon = {
                Surface(
                    shape = MaterialShapes.Clover4Leaf.toShape(),
                    // 色块直接用选中的主题色
                    color = themeColor ?: MaterialTheme.colorScheme.primary,
                    // 图标用 onPrimary，与色块形成对比，避免看不清
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 1.dp,
                ) {
                    Icon(
                        Icons.TwoTone.ColorLens,
                        contentDescription = stringResource(Res.string.favorite_color),
                        modifier = Modifier.padding(2.dp),
                    )
                }
            },
            label = {
                Text(
                    stringResource(Res.string.favorite_color),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = shapes.random(),
        )
    }
}

/**
 * 标签。
 */
@Composable
private fun Tag(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图标
            Box(
                modifier = Modifier.size(20.dp),
                propagateMinConstraints = true,
            ) {
                icon()
            }

            // 标签
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelSmall,
                content = label,
            )
        }
    }
}
