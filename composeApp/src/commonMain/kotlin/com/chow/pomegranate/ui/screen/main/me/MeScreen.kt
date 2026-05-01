package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.app_name
import pomegranate.composeapp.generated.resources.avatar
import pomegranate.composeapp.generated.resources.campus
import pomegranate.composeapp.generated.resources.campus_canton
import pomegranate.composeapp.generated.resources.favorite_color
import pomegranate.composeapp.generated.resources.gender
import pomegranate.composeapp.generated.resources.girl
import pomegranate.composeapp.generated.resources.ic_logo
import pomegranate.composeapp.generated.resources.settings
import pomegranate.composeapp.generated.resources.wallpaper

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
) {
    MeContent(
        toolbarExpanded = toolbarExpanded,
        onToolbarExpandedChange = onToolbarExpandedChange,
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
) {
    val layoutDirection = LocalLayoutDirection.current

    val wallpaperUrl = "https://cn.bing.com/th?id=OHR.CumberlandOaks_FR-FR5406318422_UHD.jpg"

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
                avatar = null,
                background = wallpaperUrl,
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
                    TagGroup(modifier = Modifier.fillMaxWidth())
                }

                item {
                    // 设置
                    OutlinedCard {
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
}

/**
 * 用户卡片。
 */
@Composable
private fun UserCard(
    avatar: Any?,
    background: Any?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 1.dp,
    ) {
        Box {
            // 背景图
            AsyncImage(
                model = background,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.36f),
                contentDescription = stringResource(Res.string.wallpaper),
                contentScale = ContentScale.Crop,
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
                ) {
                    if (avatar == null) {
                        // 默认头像
                        Image(
                            painterResource(Res.drawable.ic_logo),
                            contentDescription = stringResource(Res.string.avatar),
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
                        "00000000000",
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
                    "v1.0.0",
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
        // 校区
        Tag(
            onClick = {},
            icon = {
                Icon(
                    Icons.TwoTone.School,
                    contentDescription = stringResource(Res.string.campus),
                )
            },
            label = {
                Text(
                    stringResource(Res.string.campus_canton),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = shapes.random(),
        )

        // 性别
        Tag(
            onClick = {},
            icon = {
                Icon(
                    Icons.TwoTone.Face4,
                    contentDescription = stringResource(Res.string.gender),
                )
            },
            label = {
                Text(
                    stringResource(Res.string.girl),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = shapes.random(),
        )

        // 喜欢的颜色
        Tag(
            onClick = {},
            icon = {
                Surface(
                    shape = MaterialShapes.Clover4Leaf.toShape(),
                    contentColor = MaterialTheme.colorScheme.primary,
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