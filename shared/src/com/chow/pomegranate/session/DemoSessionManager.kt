package com.chow.pomegranate.session

import com.chow.pomegranate.repository.DemoModeRepository
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val demoUserId = MutableStateFlow(MockAcademicAffairs.USER_ID)
    private val demoAccounts = List(12) { index ->
        Account(if (index == 0) MockAcademicAffairs.USER_ID else "demo-${index.toString().padStart(2, '0')}",
            false, LoginTarget.entries[index % LoginTarget.entries.size], isDemo = true)
    }
    override val userId = combine(mode.enabled, real.userId, demoUserId) { enabled, userId, demoId ->
        when (enabled) {
            true -> demoId
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

    override val accounts = combine(mode.enabled, real.accounts, demoUserId) { enabled, accounts, demoId ->
        if (enabled == true) demoAccounts.map { it.copy(isActive = it.userId == demoId) }
            .sortedByDescending { it.isActive } else accounts
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val secondClassroomState = combine(mode.enabled, real.secondClassroomState) { enabled, state ->
        if (enabled == true) SessionState.Active else if (enabled == false) state else SessionState.Inactive
    }.stateIn(scope, SharingStarted.Eagerly, SessionState.Inactive)

    override suspend fun signIn(request: LoginRequest) {
        if (!mode.isEnabled()) real.signIn(request)
    }

    override suspend fun awaitSecondClassroomLogin(): Boolean? =
        if (mode.isEnabled()) false else real.awaitSecondClassroomLogin()

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
            check(demoAccounts.any { it.userId == userId }) { "请先关闭演示模式再切换真实账号。" }
            demoUserId.value = userId
        } else real.switchAccount(userId)
    }

    override suspend fun deleteAccount(userId: String) {
        check(demoAccounts.none { it.userId == userId }) { "演示账号不能删除。" }
        check(!mode.isEnabled()) { "请先关闭演示模式再管理真实账号。" }
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
