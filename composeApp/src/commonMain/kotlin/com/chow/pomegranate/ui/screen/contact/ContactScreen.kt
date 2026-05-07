package com.chow.pomegranate.ui.screen.contact

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Campus
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.campus_canton
import pomegranate.composeapp.generated.resources.campus_foshan
import pomegranate.composeapp.generated.resources.emergency_contact

/**
 * 紧急电话页导航入口。
 */
fun EntryProviderScope<NavKey>.contactEntry() {
    entry<ContactRoute> {
        ContactScreen()
    }
}

/**
 * 紧急电话页路由。
 */
@Serializable
data object ContactRoute : NavKey

/**
 * 紧急电话页。
 */
@Composable
private fun ContactScreen(
    modifier: Modifier = Modifier,
) {
    ContactContent(
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ContactContentPreview() {
    PomegranateExpressiveTheme {
        ContactContent()
    }
}

@Composable
private fun ContactContent(
    modifier: Modifier = Modifier,
) {
    val backStack = LocalBackStack.current

    // 高斯模糊
    val sky = rememberSky()

    val tabs = Campus.entries

    val pagerState = rememberPagerState { tabs.size }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                tabs = tabs,
                pagerState = pagerState,
                onBack = {
                    backStack.removeLast()
                },
                modifier = Modifier.cloudy(sky = sky),
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val tab = tabs[page]

            // 紧急联系电话
            val emergencyContacts = when (tab) {
                Campus.Canton -> EmergencyContact.Canton.entries
                Campus.Foshan -> EmergencyContact.Foshan.entries
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.sky(sky = sky),
                contentPadding = innerPadding + PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(emergencyContacts) { contact ->
                    ContactCard(
                        onClick = {},
                        department = {
                            // 部门名称
                            Text(stringResource(contact.department))
                        },
                        businessHours = contact.businessHours?.run {
                            {
                                // 工作时间
                                Text(
                                    "$start-$endInclusive"
                                )
                            }
                        },
                        phoneNumber = {
                            // 联系电话
                            Text(contact.phoneNumber)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 顶部导航。
 */
@Composable
private fun TopBar(
    tabs: List<Campus>,
    pagerState: PagerState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface.copy(0.8f),
        modifier = modifier,
    ) {
        Column(
            modifier = modifier,
        ) {
            PomTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.emergency_contact),
                    )
                },
                modifier = modifier,
                navigationIcon = {
                    // 返回按钮
                    BackIconButton(
                        onClick = onBack,
                    )
                },
                shadowElevation = 0.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )

            // 标签行
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                tabs.forEachIndexed { index, campus ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                stringResource(
                                    when (campus) {
                                        Campus.Canton -> Res.string.campus_canton
                                        Campus.Foshan -> Res.string.campus_foshan
                                    }
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * 联系电话卡片。
 */
@Composable
private fun ContactCard(
    onClick: () -> Unit,
    department: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    businessHours: @Composable (() -> Unit)? = null,
    phoneNumber: @Composable (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primaryFixed,
        ),
    ) {
        ListItem(
            headlineContent = {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    content = department,
                )
            },
            overlineContent = {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.tertiary,
                ) {
                    businessHours?.invoke()
                }
            },
            supportingContent = phoneNumber,
        )
    }
}