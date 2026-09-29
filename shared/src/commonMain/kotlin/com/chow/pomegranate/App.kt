package com.chow.pomegranate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.di.AppModule
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.popDestination
import com.chow.pomegranate.ui.screen.account.AccountRoute
import com.chow.pomegranate.ui.screen.account.accountEntry
import com.chow.pomegranate.ui.screen.cet.CetRoute
import com.chow.pomegranate.ui.screen.cet.cetEntry
import com.chow.pomegranate.ui.screen.contact.ContactRoute
import com.chow.pomegranate.ui.screen.contact.contactEntry
import com.chow.pomegranate.ui.screen.exam.ExamRoute
import com.chow.pomegranate.ui.screen.exam.examEntry
import com.chow.pomegranate.ui.screen.feature.AcademicProgressRoute
import com.chow.pomegranate.ui.screen.feature.AcademicProgramRoute
import com.chow.pomegranate.ui.screen.feature.AcademicPlanRoute
import com.chow.pomegranate.ui.screen.feature.CourseSelectionRoute
import com.chow.pomegranate.ui.screen.feature.CourseOperationRoute
import com.chow.pomegranate.ui.screen.feature.CourseTimetableRoute
import com.chow.pomegranate.ui.screen.feature.ExtracurricularRoute
import com.chow.pomegranate.ui.screen.feature.FeatureDetailRoute
import com.chow.pomegranate.ui.screen.feature.TeachersRoute
import com.chow.pomegranate.ui.screen.feature.academicFeatureEntries
import com.chow.pomegranate.ui.screen.feature.academicProgramEntry
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.ui.screen.login.loginEntry
import com.chow.pomegranate.ui.screen.main.MainRoute
import com.chow.pomegranate.ui.screen.main.mainEntry
import com.chow.pomegranate.ui.screen.notice.NoticeRoute
import com.chow.pomegranate.ui.screen.notice.noticeEntry
import com.chow.pomegranate.ui.screen.otp.OtpRoute
import com.chow.pomegranate.ui.screen.otp.otpEntry
import com.chow.pomegranate.ui.screen.otp.OtpEditorRoute
import com.chow.pomegranate.ui.screen.settings.SettingsRoute
import com.chow.pomegranate.ui.screen.settings.settingsEntry
import com.chow.pomegranate.ui.screen.transcript.TranscriptRoute
import com.chow.pomegranate.ui.screen.transcript.transcriptEntry
import com.chow.pomegranate.ui.screen.websites.WebsitesRoute
import com.chow.pomegranate.ui.screen.websites.websitesEntry
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.SessionState
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import org.koin.plugin.module.dsl.modules
import com.chow.pomegranate.ui.screen.notice.noticeDetailEntry
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import com.chow.pomegranate.ui.screen.notice.NoticeDetailRoute

/** 序列化配置 */
private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute::class)
            subclass(SettingsRoute::class)
            subclass(LoginRoute::class)
            subclass(OtpRoute::class)
            subclass(ContactRoute::class)
            subclass(NoticeRoute::class)
            subclass(WebsitesRoute::class)
            subclass(ExamRoute::class)
            subclass(CetRoute::class)
            subclass(TranscriptRoute::class)
            subclass(CourseSelectionRoute::class)
            subclass(CourseOperationRoute::class)
            subclass(ExtracurricularRoute::class)
            subclass(TeachersRoute::class)
            subclass(CourseTimetableRoute::class)
            subclass(AcademicPlanRoute::class)
            subclass(AcademicProgressRoute::class)
            subclass(AcademicProgramRoute::class)
            subclass(FeatureDetailRoute::class)
            subclass(OtpEditorRoute::class)
            subclass(AccountRoute::class)
            subclass(NoticeDetailRoute::class)
        }
    }
}

@OptIn(InternalSerializationApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Preview
fun App(
    platformKoinConfiguration: KoinAppDeclaration? = null,
    onThemeResolved: ((background: Color, isDark: Boolean) -> Unit)? = null,
) {
    KoinApplication(
        configuration = koinConfiguration {
            platformKoinConfiguration?.invoke(this)
            modules(AppModule::class)
        },
    ) {
        PomegranateExpressiveTheme(onThemeResolved = onThemeResolved) {
            // 弹窗场景必须排在非覆盖场景之前
            val dialogSceneStrategy = remember { DialogSceneStrategy<NavKey>() }
            // 列表-详情自适应：宽屏并排，窄屏单栏
            val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>()
            val snackbarHostState = remember { SnackbarHostState() }
            val backStack = rememberNavBackStack(
                configuration = savedStateConfiguration,
                MainRoute,
            )

            // 会话管理器
            val sessionManager = koinInject<SessionManager>()
            val sessionState by sessionManager.state.collectAsState()

            // 登录过期提示
            val sessionExpiredMessage = stringResource(Res.string.session_expired)

            // 启动时自动登录
            LaunchedEffect(Unit) {
                sessionManager.autoLogin()
            }

            // 登录状态过期时回到登录页
            LaunchedEffect(sessionState) {
                if (sessionState is SessionState.Expired) {
                    // 从栈中移除已有的登录页，避免重复添加
                    backStack.removeAll { it is LoginRoute }
                    backStack.add(LoginRoute)

                    snackbarHostState.showSnackbar(
                        message = sessionExpiredMessage,
                    )
                }
            }

            CompositionLocalProvider(
                LocalBackStack provides backStack,
                LocalSnackbarHostState provides snackbarHostState,
            ) {
                // 根布局自己铺主题背景色：NavDisplay 转场时两侧场景可能都还没画上，
                // 不铺的话会直接露出窗口背景（深色模式下就是那一闪的白）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.popDestination() },
                        sceneStrategies = listOf(dialogSceneStrategy, listDetailSceneStrategy),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            // 主页
                            mainEntry()
                            // 设置页
                            settingsEntry()
                            // 登录页
                            loginEntry()
                            // OTP 页
                            otpEntry()
                            // 紧急电话页
                            contactEntry()
                            // 教学通知页
                            noticeEntry()
                            // 教学通知详情页
                            noticeDetailEntry()
                            // 常用网站页
                            websitesEntry()
                            // 考试安排页
                            examEntry()
                            // CET 页
                            cetEntry()
                            // 课程成绩页
                            transcriptEntry()
                            // 账号管理弹窗
                            accountEntry()
                            // 学业功能页和详情弹窗
                            academicFeatureEntries()
                            // 培养方案页
                            academicProgramEntry()
                        },
                    )

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
