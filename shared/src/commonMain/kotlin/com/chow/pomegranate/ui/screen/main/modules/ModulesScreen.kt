package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.cet.CetRoute
import com.chow.pomegranate.ui.screen.contact.ContactRoute
import com.chow.pomegranate.ui.screen.exam.ExamRoute
import com.chow.pomegranate.ui.screen.feature.AcademicPlanRoute
import com.chow.pomegranate.ui.screen.feature.AcademicProgramRoute
import com.chow.pomegranate.ui.screen.feature.AcademicProgressRoute
import com.chow.pomegranate.ui.screen.feature.CourseSelectionRoute
import com.chow.pomegranate.ui.screen.feature.CourseTimetableRoute
import com.chow.pomegranate.ui.screen.feature.ExtracurricularRoute
import com.chow.pomegranate.ui.screen.feature.TeachersRoute
import com.chow.pomegranate.ui.screen.notice.NoticeRoute
import com.chow.pomegranate.ui.screen.otp.OtpRoute
import com.chow.pomegranate.ui.screen.transcript.TranscriptRoute
import com.chow.pomegranate.ui.screen.websites.WebsitesRoute
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.save_failed
import com.chow.pomegranate.close
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import com.chow.pomegranate.Res

/**
 * 模块页路由。
 */
@Serializable
data object ModulesRoute : NavKey

/**
 * 模块页。
 */
@Composable
fun ModulesScreen() {
    val preferenceRepository = koinInject<AppPreferenceRepository>()

    // 回调（launch 内）不是 @Composable 上下文，文案要提前取好
    val saveFailedMessage = stringResource(Res.string.save_failed)
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    ModulesContent(
        onModuleOpened = { module ->
            scope.launch {
                runCatching {
                    preferenceRepository.set(PreferenceKeys.LAST_MODULE, module.name)
                }.onFailure { throwable ->
                    snackbarHostState.showSnackbar(
                        throwable.message ?: saveFailedMessage,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun ModulesContentPreview() {
    PomegranateExpressiveTheme {
        ModulesContent()
    }
}

/**
 * 模块页内容。
 */
@Composable
private fun ModulesContent(
    modifier: Modifier = Modifier,
    onModuleOpened: (Module) -> Unit = {},
) {
    val backStack = LocalBackStack.current
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val minimumItemWidth = if (
        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    ) {
        240.dp
    } else {
        152.dp
    }

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minimumItemWidth),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalItemSpacing = 16.dp,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(Module.entries) { module ->
                val moduleLabel = stringResource(module.label)

                ModuleItem(
                    onClick = {
                        onModuleOpened(module)
                        backStack.add(module.route())
                    },
                    headlineContent = {
                        Text(
                            moduleLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        when (module) {
                            else -> Unit
                        }
                    },
                    trailingContent = {
                        Icon(
                            module.icon,
                            contentDescription = moduleLabel,
                            tint = MaterialTheme.colorScheme.inversePrimary,
                        )
                    },
                )
            }
        }
    }
}

/**
 * 模块项。
 */
@Composable
private fun ModuleItem(
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
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
            headlineContent = headlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            tonalElevation = 1.dp,
        )
    }
}

/**
 * 返回模块对应的路由。
 */
private fun Module.route(): NavKey = when (this) {
    Module.OTP -> OtpRoute
    Module.EmergencyContact -> ContactRoute
    Module.AcademicNotice -> NoticeRoute
    Module.UsefulWebsites -> WebsitesRoute
    Module.ExamSchedule -> ExamRoute
    Module.CET -> CetRoute
    Module.Transcript -> TranscriptRoute
    Module.CourseSelection -> CourseSelectionRoute
    Module.Extracurricular -> ExtracurricularRoute
    Module.Teachers -> TeachersRoute
    Module.CourseTimetable -> CourseTimetableRoute
    Module.AcademicPlan -> AcademicPlanRoute
    Module.AcademicProgress -> AcademicProgressRoute
    Module.AcademicProgram -> AcademicProgramRoute
}
