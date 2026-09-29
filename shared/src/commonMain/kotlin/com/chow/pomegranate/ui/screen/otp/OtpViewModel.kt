package com.chow.pomegranate.ui.screen.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.data.local.OtpAccountEntity
import com.chow.pomegranate.repository.OtpAccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** OTP 页面状态持有者。 */
@KoinViewModel
class OtpViewModel(
    private val repository: OtpAccountRepository,
) : ViewModel() {
    /** 当前账户列表。 */
    val accounts: StateFlow<List<OtpAccountEntity>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** 删除账户。 */
    fun delete(account: OtpAccountEntity, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.delete(account) })
        }
    }
}
