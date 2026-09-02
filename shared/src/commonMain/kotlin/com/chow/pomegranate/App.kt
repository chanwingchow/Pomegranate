package com.chow.pomegranate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chow.pomegranate.di.AppModule
import com.chow.pomegranate.ui.navigation.LocalBackStack
import com.chow.pomegranate.ui.screen.cet.CetRoute
import com.chow.pomegranate.ui.screen.cet.cetEntry
import com.chow.pomegranate.ui.screen.contact.ContactRoute
import com.chow.pomegranate.ui.screen.contact.contactEntry
import com.chow.pomegranate.ui.screen.exam.ExamRoute
import com.chow.pomegranate.ui.screen.exam.examEntry
import com.chow.pomegranate.ui.screen.login.LoginRoute
import com.chow.pomegranate.ui.screen.login.loginEntry
import com.chow.pomegranate.ui.screen.main.MainRoute
import com.chow.pomegranate.ui.screen.main.mainEntry
import com.chow.pomegranate.ui.screen.notice.NoticeRoute
import com.chow.pomegranate.ui.screen.notice.noticeEntry
import com.chow.pomegranate.ui.screen.otp.OtpRoute
import com.chow.pomegranate.ui.screen.otp.otpEntry
import com.chow.pomegranate.ui.screen.settings.SettingsRoute
import com.chow.pomegranate.ui.screen.settings.settingsEntry
import com.chow.pomegranate.ui.screen.websites.WebsitesRoute
import com.chow.pomegranate.ui.screen.websites.websitesEntry
import com.chow.pomegranate.ui.theme.PomegranateExpressiveTheme
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinConfiguration
import org.koin.plugin.module.dsl.modules

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
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
@Preview
fun App(
    platformKoinConfiguration: KoinAppDeclaration? = null,
) {
    KoinApplication(
        configuration = koinConfiguration {
            platformKoinConfiguration?.invoke(this)
            modules(AppModule::class)
        },
    ) {
        PomegranateExpressiveTheme {
            val backStack = rememberNavBackStack(
                configuration = savedStateConfiguration,
                MainRoute,
            )

            CompositionLocalProvider(
                LocalBackStack provides backStack,
            ) {
                NavDisplay(
                    backStack = backStack,
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
                        // 常用网站页
                        websitesEntry()
                        // 考试安排页
                        examEntry()
                        // CET 页
                        cetEntry()
                    },
                )
            }
        }
    }
}