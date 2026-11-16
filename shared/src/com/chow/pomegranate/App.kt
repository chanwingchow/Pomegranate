package com.chow.pomegranate

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.chow.pomegranate.ui.component.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.chow.pomegranate.ui.component.LocalAppCoroutineScope
import androidx.compose.runtime.key
import com.chow.pomegranate.ui.component.PomLoadingIndicator
import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.repository.ClassroomReplacementRepository
import com.chow.pomegranate.repository.ClassroomReplacements
import com.chow.pomegranate.ui.component.LocalClassroomReplacements
import com.chow.pomegranate.ui.component.LocalCourseNameReplacements
import com.chow.pomegranate.repository.NameReplacementTarget
import com.chow.pomegranate.ui.component.DemoModeBadge
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import com.chow.pomegranate.ui.navigation.FullWidthOverviewSceneStrategy
import com.chow.pomegranate.ui.navigation.BottomSheetSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.di.AppModule
import com.chow.pomegranate.ui.component.LocalSnackbarHostState
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.navigation.popDestination
import com.chow.pomegranate.ui.screen.account.AccountConfirmRoute
import com.chow.pomegranate.ui.screen.account.AccountRoute
import com.chow.pomegranate.ui.screen.account.AccountNicknameRoute
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
import com.chow.pomegranate.ui.screen.feature.CourseBasketRoute
import com.chow.pomegranate.ui.screen.feature.CourseFiltersRoute
import com.chow.pomegranate.ui.screen.feature.SchoolTimetableFiltersRoute
import com.chow.pomegranate.ui.screen.feature.SchoolSemesterRoute
import com.chow.pomegranate.ui.screen.feature.SchoolTimetableDataRoute
import com.chow.pomegranate.ui.screen.feature.SchoolTimetableDeleteRoute
import com.chow.pomegranate.ui.screen.feature.TeacherSearchRoute
import com.chow.pomegranate.ui.screen.settings.WeekSettingRoute
import com.chow.pomegranate.ui.screen.settings.ClassroomRulesRoute
import com.chow.pomegranate.ui.screen.settings.ClassroomRuleEditorRoute
import com.chow.pomegranate.ui.screen.settings.ClassroomRuleDeleteRoute
import com.chow.pomegranate.ui.screen.settings.ClassroomBuiltinRulesRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableNotesRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableCourseRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableWeekRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableDateRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableEditRoute
import com.chow.pomegranate.ui.screen.main.timetable.rememberTimetableCourseSceneStrategy
import com.chow.pomegranate.ui.screen.feature.CourseInstructionsRoute
import com.chow.pomegranate.ui.screen.cet.CetNoteRoute
import com.chow.pomegranate.ui.screen.login.SecondClassLoginRoute
import com.chow.pomegranate.ui.screen.feature.CourseTimetableRoute
import com.chow.pomegranate.ui.screen.feature.ExtracurricularRoute
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.screen.feature.FeatureDetailRoute
import com.chow.pomegranate.ui.screen.feature.TeachersRoute
import com.chow.pomegranate.ui.screen.feature.academicFeatureEntries
import com.chow.pomegranate.ui.screen.feature.academicProgramEntry
import com.chow.pomegranate.ui.screen.login.LoginErrorRoute
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.ui.screen.login.loginEntry
import com.chow.pomegranate.ui.screen.main.me.ThemeColorRoute
import com.chow.pomegranate.ui.screen.main.me.AvatarActionsRoute
import com.chow.pomegranate.ui.screen.main.me.meDialogEntries
import com.chow.pomegranate.ui.screen.main.MainRoute
import com.chow.pomegranate.ui.screen.main.mainEntry
import com.chow.pomegranate.ui.screen.notice.NoticeRoute
import com.chow.pomegranate.ui.screen.notice.noticeEntry
import com.chow.pomegranate.ui.screen.otp.OtpRoute
import com.chow.pomegranate.ui.screen.otp.otpEntry
import com.chow.pomegranate.ui.screen.otp.OtpDeleteRoute
import com.chow.pomegranate.ui.screen.otp.OtpEditorRoute
import com.chow.pomegranate.ui.screen.settings.SettingsRoute
import com.chow.pomegranate.ui.screen.settings.settingsEntry
import com.chow.pomegranate.ui.screen.transcript.TranscriptDetailRoute
import com.chow.pomegranate.ui.screen.transcript.TranscriptOverviewRoute
import com.chow.pomegranate.ui.navigation.DialogListDetailSceneStrategy
import com.chow.pomegranate.ui.component.AdaptiveStudyDetailRoute
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
internal val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ThemeColorRoute::class)
            subclass(AvatarActionsRoute::class)
            subclass(MainRoute::class)
            subclass(SettingsRoute::class)
            subclass(WeekSettingRoute::class)
            subclass(ClassroomRulesRoute::class)
            subclass(ClassroomRuleEditorRoute::class)
            subclass(ClassroomRuleDeleteRoute::class)
            subclass(ClassroomBuiltinRulesRoute::class)
            subclass(SchoolSemesterRoute::class)
            subclass(SchoolTimetableDataRoute::class)
            subclass(SchoolTimetableDeleteRoute::class)
            subclass(TimetableNotesRoute::class)
            subclass(TimetableCourseRoute::class)
            subclass(TimetableEditRoute::class)
            subclass(TimetableWeekRoute::class)
            subclass(TimetableDateRoute::class)
            subclass(CourseFiltersRoute::class)
            subclass(SchoolTimetableFiltersRoute::class)
            subclass(TeacherSearchRoute::class)
            subclass(LoginRoute::class)
            subclass(LoginErrorRoute::class)
            subclass(SecondClassLoginRoute::class)
            subclass(OtpRoute::class)
            subclass(ContactRoute::class)
            subclass(NoticeRoute::class)
            subclass(WebsitesRoute::class)
            subclass(ExamRoute::class)
            subclass(CetRoute::class)
            subclass(CetNoteRoute::class)
            subclass(TranscriptRoute::class)
            subclass(TranscriptDetailRoute::class)
            subclass(TranscriptOverviewRoute::class)
            subclass(CourseSelectionRoute::class)
            subclass(CourseOperationRoute::class)
            subclass(CourseBasketRoute::class)
            subclass(CourseInstructionsRoute::class)
            subclass(ExtracurricularRoute::class)
            subclass(TeachersRoute::class)
            subclass(CourseTimetableRoute::class)
            subclass(AcademicPlanRoute::class)
            subclass(AcademicProgressRoute::class)
            subclass(AcademicProgramRoute::class)
            subclass(StudyDetailRoute::class)
            subclass(AdaptiveStudyDetailRoute::class)
            subclass(FeatureDetailRoute::class)
            subclass(OtpDeleteRoute::class)
            subclass(OtpEditorRoute::class)
            subclass(AccountConfirmRoute::class)
            subclass(AccountRoute::class)
            subclass(AccountNicknameRoute::class)
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
            val demoModeRepository = koinInject<DemoModeRepository>()
            val classroomRepository = koinInject<ClassroomReplacementRepository>()
            val classroomRules by classroomRepository.rules.collectAsState(initial = emptyList())
            val classrooms = remember(classroomRules) { ClassroomReplacements(classroomRules) }
            val courseNames = remember(classroomRules) { ClassroomReplacements(classroomRules, NameReplacementTarget.Course) }
            val demoEnabled by demoModeRepository.enabled.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val feedbackScope = rememberCoroutineScope()
            LaunchedEffect(Unit) { demoModeRepository.isEnabled() }
            if (demoEnabled == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PomLoadingIndicator()
                }
                return@PomegranateExpressiveTheme
            }

            // 模式切换保留所在页面、滚动位置与主页选中项。
            val backStack = rememberNavBackStack(configuration = savedStateConfiguration, MainRoute)
            val saveableEntryDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()

            // 切换数据作用域时保留 NavDisplay，避免重复注册页面的保存状态。
            val modeViewModels = rememberViewModelStoreProvider(key = "pomegranate-mode:$demoEnabled")
            val viewModelEntryDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>(modeViewModels)
            // 弹窗场景必须排在非覆盖场景之前
            val timetableSceneStrategy = rememberTimetableCourseSceneStrategy()
            val dialogSceneStrategy = remember { DialogSceneStrategy<NavKey>() }
            val bottomSheetSceneStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
            // 列表-详情自适应：宽屏并排，窄屏单栏
            val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>()
            val dialogListDetailSceneStrategy = remember(listDetailSceneStrategy) { DialogListDetailSceneStrategy(listDetailSceneStrategy) }
            val overviewSceneStrategy = remember { FullWidthOverviewSceneStrategy<NavKey>() }

            // 会话管理器
            val sessionManager = koinInject<SessionManager>()
            val sessionState by sessionManager.state.collectAsState()

            // 登录过期提示
            val sessionExpiredMessage = stringResource(Res.string.session_expired)

            // 启动时自动登录
            LaunchedEffect(demoEnabled) {
                sessionManager.autoLogin()
            }

            // 登录状态过期时回到登录页
            LaunchedEffect(sessionState, demoEnabled) {
                if (demoEnabled != true && sessionState is SessionState.Expired && backStack.lastOrNull() != SettingsRoute &&
                    backStack.none { it is LoginRoute || it is SecondClassLoginRoute }) {
                    backStack.add(LoginRoute)

                    snackbarHostState.showSnackbar(
                        message = sessionExpiredMessage,
                    )
                }
            }

            CompositionLocalProvider(
                LocalAppCoroutineScope provides feedbackScope,
                LocalBackStack provides backStack,
                LocalSnackbarHostState provides snackbarHostState,
                LocalClassroomReplacements provides classrooms,
                LocalCourseNameReplacements provides courseNames,
            ) {
                // 根布局自己铺主题背景色：NavDisplay 转场时两侧场景可能都还没画上，
                // 不铺的话会直接露出窗口背景（深色模式下就是那一闪的白）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    NavDisplay(
                        // 桌面默认无转场，跨单/双栏后旧场景可能未及时释放条目；显式转场让场景完成交接。
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        popTransitionSpec = { fadeIn() togetherWith fadeOut() },
                        backStack = backStack,
                        onBack = { backStack.popDestination() },
                        sceneStrategies = listOf(timetableSceneStrategy, bottomSheetSceneStrategy, dialogListDetailSceneStrategy, dialogSceneStrategy, overviewSceneStrategy, listDetailSceneStrategy),
                        entryDecorators = listOf(
                            saveableEntryDecorator,
                            viewModelEntryDecorator,
                            rememberResultEventBusNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            // 主页
                            mainEntry()
                            meDialogEntries()
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

                    // 只在根布局绘制一次，所有页面沿用原来的导航与内容布局。
                    if (demoEnabled == true) {
                        DemoModeBadge(
                            Modifier.align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                                .padding(top = 4.dp, end = 4.dp),
                        )
                    }

                    AppSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
