package com.chow.pomegranate.session

import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 演示会话仅存在于内存中，不读写真实账号、密码或 Cookie。 */
class DemoSessionManager(
    private val real: SessionManager,
    private val demo: MockAcademicAffairs,
    private val mode: DemoModeRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SessionManager {
    override val userId = combine(mode.enabled, real.userId) { enabled, userId ->
        when (enabled) {
            true -> MockAcademicAffairs.USER_ID
            false -> userId
            null -> null
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val state = combine(mode.enabled, real.state) { enabled, state ->
        when (enabled) {
            true -> SessionState.Active
            false -> state
            null -> SessionState.Inactive
        }
    }.stateIn(scope, SharingStarted.Eagerly, SessionState.Inactive)

    override val accounts = combine(mode.enabled, real.accounts) { enabled, accounts ->
        if (enabled == true) listOf(Account(MockAcademicAffairs.USER_ID, true)) else accounts
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun getCaptcha(): ByteArray =
        if (mode.isEnabled()) demo.auth.getCaptcha() else real.getCaptcha()

    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) {
        if (!mode.isEnabled()) real.login(userId, password, secondClassroomPassword, captcha)
    }

    override suspend fun autoLogin() {
        if (!mode.isEnabled()) real.autoLogin()
    }

    override suspend fun switchAccount(userId: String) {
        if (mode.isEnabled()) {
            check(userId == MockAcademicAffairs.USER_ID) { "请先关闭演示模式再切换真实账号。" }
        } else real.switchAccount(userId)
    }

    override suspend fun deleteAccount(userId: String) {
        check(!mode.isEnabled()) { "演示模式下不能删除账号，请先关闭演示模式。" }
        real.deleteAccount(userId)
    }

    override suspend fun refresh() {
        if (!mode.isEnabled()) real.refresh()
    }

    override suspend fun logout() {
        if (mode.isEnabled()) mode.setEnabled(false) else real.logout()
    }

    override suspend fun awaitLogin(): Boolean? =
        if (mode.isEnabled()) false else real.awaitLogin()
}
