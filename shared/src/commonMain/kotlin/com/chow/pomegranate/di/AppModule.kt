package com.chow.pomegranate.di

import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.data.local.ExtracurricularSessionDao
import com.chow.pomegranate.data.local.OtpAccountDao
import com.chow.pomegranate.data.local.UserDao
import com.chow.pomegranate.data.local.createAppDatabase
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.notice.api.AcademicNotice
import com.chow.pomegranate.service.cet.api.CET
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.wallpaper.Wallpaper
import com.chow.pomegranate.session.CookieStorage
import com.chow.pomegranate.session.SecureStore
import com.chow.pomegranate.session.SessionApiClient
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.baseAcademicAffairsHttpClient
import com.chow.pomegranate.session.createSecureStore
import com.chow.pomegranate.session.sessionAcademicAffairsHttpClient
import com.chow.pomegranate.tool.ocr.OcrEngine
import com.chow.pomegranate.tool.ocr.createCaptchaOcrEngine
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

/** 认证用的教务客户端限定符，见 [AppModule.authAcademicAffairs]。 */
const val AUTH_ACADEMIC_AFFAIRS = "authAcademicAffairs"

@Module
@ComponentScan("com.chow.pomegranate")
class AppModule {
    @Singleton
    fun database(): AppDatabase = createAppDatabase()

    @Singleton
    fun otpAccountDao(database: AppDatabase): OtpAccountDao = database.otpAccountDao()

    @Singleton
    fun userDao(database: AppDatabase): UserDao = database.userDao()

    @Singleton
    fun extracurricularSessionDao(database: AppDatabase): ExtracurricularSessionDao =
        database.extracurricularSessionDao()

    @Singleton
    fun cookieStorage(): CookieStorage = CookieStorage()

    /**
     * 原始客户端，用于登录、登出与会话校验，避免递归重新登录。
     *
     * 包装为 [SessionApiClient]，以免与业务客户端同类型冲突。
     */
    @Singleton
    fun sessionApiClient(cookieStorage: CookieStorage): SessionApiClient =
        SessionApiClient(baseAcademicAffairsHttpClient(cookieStorage))

    /** 业务客户端，登录失效时自动重新登录并重放请求。 */
    @Singleton
    fun httpClient(
        cookieStorage: CookieStorage,
        sessionManager: Lazy<SessionManager>,
    ): HttpClient = sessionAcademicAffairsHttpClient(cookieStorage, sessionManager)

    @Singleton
    fun ocrEngine(): OcrEngine = createCaptchaOcrEngine()

    @Singleton
    fun secureStore(): SecureStore = createSecureStore()

    @Singleton
    fun academicAffairs(client: HttpClient): AcademicAffairs = AcademicAffairs(client)

    /**
     * 供登录、登出与验证码使用的教务客户端实例。
     *
     * 必须走 [SessionApiClient] 里的原始客户端：登录与会话校验本身如果经过会话插件，
     * 插件会先等待登录成功再放行请求，而登录又依赖这些请求，形成自锁——
     * 表现为验证码迟迟加载不出来（等满 15 秒后报错），重新登录永远无法完成。
     *
     * 与业务实例共用同一个 [CookieStorage]，保证验证码下发的 Cookie 能被后续登录复用。
     */
    @Singleton
    @Named(value = AUTH_ACADEMIC_AFFAIRS)
    fun authAcademicAffairs(cookieStorage: CookieStorage): AcademicAffairs =
        AcademicAffairs(baseAcademicAffairsHttpClient(cookieStorage))

    /** 认证模块，供会话管理器使用；必须绕过会话校验插件。 */
    @Singleton
    fun academicAffairsAuth(
        @Named(value = AUTH_ACADEMIC_AFFAIRS) academicAffairs: AcademicAffairs,
    ): AcademicAffairs.Auth = academicAffairs.auth

    /** 学业模块，供培养方案等仓库使用。 */
    @Singleton
    fun academicAffairsAcademic(academicAffairs: AcademicAffairs): AcademicAffairs.Academic =
        academicAffairs.academic

    @Singleton
    fun extracurricularActivity(): ExtracurricularActivity = ExtracurricularActivity()

    @Singleton
    fun academicNotice(): AcademicNotice = AcademicNotice()

    @Singleton
    fun wallpaper(): Wallpaper = Wallpaper()

    @Singleton
    fun cet(): CET = CET()
}
