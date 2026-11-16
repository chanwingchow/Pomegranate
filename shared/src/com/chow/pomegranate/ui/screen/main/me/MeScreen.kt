package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import com.chow.pomegranate.ui.component.ExpressiveActionCard
import com.chow.pomegranate.ui.component.ExpressiveIconBadge
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
import com.chow.pomegranate.ui.screen.account.AccountNicknameRoute
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.text.style.TextOverflow
import com.chow.pomegranate.ui.screen.settings.SettingsRoute
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.wallpaper
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.save
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import com.chow.pomegranate.ui.component.PomTextField
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
    val accounts = viewModel?.sessionManager?.accounts?.collectAsState()?.value.orEmpty()
    val nickname = viewModel?.nickname?.collectAsState()?.value

    // 性别
    val gender = viewModel?.gender?.collectAsState()?.value ?: Gender.Default

    // 校区
    val campus = viewModel?.campus?.collectAsState()?.value ?: CampusOption.Default

    // 主题色，null 表示默认色
    val themeColor = viewModel?.themeColor?.collectAsState()?.value

    // 主题色选择弹窗


    // 每日壁纸
    val wallpaperUrl = viewModel?.wallpaperUrl?.collectAsState()?.value

    // 头像是应用私有文件的 URI，不是远端账户资料。
    val avatar = viewModel?.avatarUri?.collectAsState()?.value
    Scaffold(modifier = modifier) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection)), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { onToolbarExpandedChange(true) },
                    onCollapse = { onToolbarExpandedChange(false) },
                ),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 16.dp + innerPadding.calculateBottomPadding() + with(FloatingToolbarDefaults) { ContainerSize + ScreenOffset }),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "profile") {
                    UserCard(onClick = { backStack.add(if (userId == null) LoginRoute else AccountRoute) },
                        avatar = avatar, onAvatarClick = { backStack.add(AvatarActionsRoute) },
                        background = wallpaperUrl, userId = userId, nickname = nickname,
                        onEditNickname = userId?.let { id -> { backStack.add(AccountNicknameRoute(id, accounts.firstOrNull { it.userId == id }?.isDemo == true, nickname.orEmpty())) } },
                        modifier = Modifier.fillMaxWidth().height(180.dp))
                }
                item(key = "palette") {
                    MeActionRow(stringResource(Res.string.favorite_color), Icons.TwoTone.ColorLens, 2) {
                        backStack.add(ThemeColorRoute(themeColor?.toHexString()))
                    }
                }
                item(key = "preferences") {
                    BoxWithConstraints(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)) {
                        val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f
                        val tiles: @Composable (Modifier) -> Unit = { tileModifier ->
                            MeToggleTile("校区", stringResource(campus.label), Icons.TwoTone.School, 0,
                                { viewModel?.toggleCampus() }, tileModifier)
                            MeToggleTile("性别", stringResource(gender.label), gender.icon, 1,
                                { viewModel?.toggleGender() }, tileModifier)
                        }
                        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { tiles(Modifier.fillMaxWidth()) }
                        else Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { tiles(Modifier.weight(1f)) }
                    }
                }
                item(key = "settings") {
                    MeActionRow(stringResource(Res.string.settings), Icons.Outlined.Settings, 1) { backStack.add(SettingsRoute) }
                }
            }
        }
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
    nickname: String? = null,
    onEditNickname: (() -> Unit)? = null,
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 昵称
                    Row(modifier = if (onEditNickname != null) Modifier.clickable(onClick = onEditNickname, onClickLabel = "修改昵称") else Modifier,
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        nickname ?: stringResource(Res.string.app_name),
                        modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (onEditNickname != null) Icon(Icons.Rounded.Edit, "修改昵称", Modifier.size(16.dp))
                    }

                    // 学号
                    Text(
                        userId ?: stringResource(Res.string.login),
                        modifier = Modifier.clickable(onClickLabel = if (userId == null) "登录" else "账号管理", onClick = onClick),
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

@Composable
private fun MeActionRow(title: String, icon: ImageVector, variant: Int, onClick: () -> Unit) {
    com.chow.pomegranate.ui.component.PomSettingRow(title, icon, variant, onClick = onClick)
}

@Composable
private fun MeToggleTile(title: String, value: String, icon: ImageVector, variant: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Surface(onClick, modifier, interactionSource = interactions, shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpressiveIconBadge(icon, variant, pressed, Modifier.size(28.dp), iconSize = 18.dp)
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
