package com.chow.pomegranate.ui.screen.feature

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_program
import com.chow.pomegranate.refresh
import com.chow.pomegranate.ui.component.BackIconButton
import com.chow.pomegranate.ui.component.ErrorContent
import com.chow.pomegranate.ui.component.LoadingContent
import com.chow.pomegranate.ui.component.PomTopAppBar
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import de.charlex.compose.htmltext.material3.HtmlText
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.chow.pomegranate.ui.component.NotLoggedInContent
import com.chow.pomegranate.ui.screen.login.LoginRoute

/**
 * 培养方案页路由。
 */
@Serializable
data object AcademicProgramRoute : NavKey

/**
 * 注册培养方案页导航入口。
 */
fun EntryProviderScope<NavKey>.academicProgramEntry() {
    entry<AcademicProgramRoute> {
        AcademicProgramScreen()
    }
}

/**
 * 培养方案页。
 */
@Composable
private fun AcademicProgramScreen(
    modifier: Modifier = Modifier,
    viewModel: AcademicProgramViewModel = koinViewModel(),
) {
    AcademicProgramContent(
        viewModel = viewModel,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AcademicProgramContentPreview() {
    PomegranateExpressiveTheme {
        AcademicProgramContent()
    }
}

/**
 * 培养方案页内容。
 */
@Composable
private fun AcademicProgramContent(
    modifier: Modifier = Modifier,
    viewModel: AcademicProgramViewModel? = null,
) {
    val backStack = LocalBackStack.current

    // 打开培养方案中的链接
    val uriHandler = LocalUriHandler.current

    // UI 状态
    val uiState = viewModel?.uiState?.collectAsState()?.value

    Scaffold(
        modifier = modifier,
        topBar = {
            PomTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.academic_program),
                    )
                },
                navigationIcon = {
                    // 返回按钮
                    BackIconButton(
                        onClick = { backStack.removeLastOrNull() },
                    )
                },
                actions = {
                    // 刷新
                    FilledTonalIconButton(
                        onClick = { viewModel?.refresh() },
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentPadding = innerPadding + PaddingValues(16.dp)

        when (uiState) {
            is AcademicProgramUiState.NotLoggedIn -> {
                // 未登录，不请求受保护接口，直接引导登录
                NotLoggedInContent(
                    onLogin = { backStack.add(LoginRoute) },
                    contentPadding = contentPadding,
                )
            }

            is AcademicProgramUiState.Error -> {
                // 加载失败
                ErrorContent(
                    throwable = uiState.throwable,
                    contentPadding = contentPadding,
                )
            }

            is AcademicProgramUiState.Success -> {
                // 培养方案内容
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                ) {
                    item {
                        HtmlText(
                            text = uiState.html,
                            style = MaterialTheme.typography.bodyMedium,
                            onUriClick = { uriHandler.openUri(it) },
                        )
                    }
                }
            }

            else -> {
                // 加载中
                LoadingContent(
                    contentPadding = contentPadding,
                )
            }
        }
    }
}
