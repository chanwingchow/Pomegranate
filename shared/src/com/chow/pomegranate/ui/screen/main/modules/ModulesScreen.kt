package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.Res
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.modules_academic
import com.chow.pomegranate.modules_tools
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.save_failed
import com.chow.pomegranate.ui.component.ExpressiveIconBadge
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Serializable
data object ModulesRoute : NavKey

@Composable
fun ModulesScreen() {
    val preferences = koinInject<AppPreferenceRepository>()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val saveFailed = stringResource(Res.string.save_failed)
    ModulesContent { module ->
        scope.launch {
            runCatching { preferences.set(PreferenceKeys.LAST_MODULE, module.name) }
                .onFailure { snackbar.showSnackbar(it.message ?: saveFailed) }
        }
    }
}

/** 按业务关系连接入口，连接组内各功能依旧独立导航。 */
@Composable
private fun ModulesContent(onModuleOpened: (Module) -> Unit = {}) {
    val backStack = LocalBackStack.current
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    val tools = listOf(Module.OTP, Module.EmergencyContact, Module.UsefulWebsites)
    val academic = listOf(
        listOf(Module.ExamSchedule, Module.CET),
        listOf(Module.CourseSelection),
        listOf(Module.CourseTimetable),
        listOf(Module.Transcript),
        listOf(Module.AcademicNotice),
        listOf(Module.Teachers),
        listOf(Module.Extracurricular),
        listOf(Module.AcademicPlan),
        listOf(Module.AcademicProgress, Module.AcademicProgram),
    )
    val open: (Module) -> Unit = { module -> onModuleOpened(module); backStack.add(module.route()) }
    Scaffold { innerPadding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp * fontScale),
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(Res.string.modules_tools), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                }
                items(tools, key = { it.name }) { module ->
                    ModuleItem(module, onClick = { open(module) })
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(Res.string.modules_academic), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp))
                }
                items(academic, key = { group -> group.joinToString { it.name } }, span = { group ->
                    GridItemSpan(when {
                        group.size >= 3 -> group.size.coerceAtMost(maxLineSpan)
                        group.singleOrNull() == Module.CourseSelection -> 2.coerceAtMost(maxLineSpan)
                        else -> group.size.coerceAtMost(maxLineSpan)
                    })
                }) { group ->
                    if (group.size == 1) ModuleItem(group.single(), onClick = { open(group.single()) },
                        wide = group.single() == Module.CourseSelection)
                    else ConnectedModules(group, open)
                }
            }
        }
    }
}

private enum class ModuleConnection { Single, Leading, Middle, Trailing }

/** 使用 Material3 连接式按钮组的形状与间距 token，外侧圆角、内侧连接。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectedModules(modules: List<Module>, onOpen: (Module) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        modules.forEachIndexed { index, module ->
            ModuleItem(
                module = module,
                onClick = { onOpen(module) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                connection = when (index) {
                    0 -> ModuleConnection.Leading
                    modules.lastIndex -> ModuleConnection.Trailing
                    else -> ModuleConnection.Middle
                },
            )
        }
    }
}

/** 按压轻微收缩，标识使用 Material 官方形状向圆形变换，松手弹回。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleItem(
    module: Module,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    connection: ModuleConnection = ModuleConnection.Single,
    wide: Boolean = false,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "Module press")
    val colors = MaterialTheme.colorScheme
    // 组外轮廓和普通入口使用同一主题圆角；仅内侧连接角更小。
    val outer = MaterialTheme.shapes.large
    val inner = MaterialTheme.shapes.extraSmall
    val connectedShape = when (connection) {
        ModuleConnection.Leading -> outer.copy(topEnd = inner.topEnd, bottomEnd = inner.bottomEnd)
        ModuleConnection.Trailing -> outer.copy(topStart = inner.topStart, bottomStart = inner.bottomStart)
        ModuleConnection.Middle -> inner
        ModuleConnection.Single -> outer
    }
    val badge: @Composable () -> Unit = {
        ExpressiveIconBadge(module.icon, module.ordinal, pressed, Modifier.size(44.dp), iconSize = 24.dp)
    }
    FilledTonalButton(
        onClick = onClick,
        interactionSource = interactions,
        shapes = ButtonDefaults.shapes(connectedShape, connectedShape),
        modifier = modifier.fillMaxWidth().heightIn(min = 90.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        contentPadding = PaddingValues(horizontal = if (wide) 12.dp else 4.dp, vertical = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurface,
        ),
    ) {
        if (wide) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                badge()
                Text(stringResource(module.label), style = MaterialTheme.typography.labelLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                badge()
                Text(stringResource(module.label), style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
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
