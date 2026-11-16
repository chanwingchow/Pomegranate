package com.chow.pomegranate.ui.screen.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.repository.AccountNicknameRepository
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

@Serializable data class AccountNicknameRoute(val userId: String, val isDemo: Boolean = false, val nickname: String = "") : NavKey

@KoinViewModel
class AccountNicknameViewModel(private val repository: AccountNicknameRepository, private val session: SessionManager) : ViewModel() {
    fun save(route: AccountNicknameRoute, nickname: String, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                check(session.accounts.value.any { it.userId == route.userId && it.isDemo == route.isDemo }) { "账号已移除，请返回后重试。" }
                repository.save(route.userId, route.isDemo, nickname)
                onResult(null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { onResult(failure) }
        }
    }
}

@Composable
internal fun AccountNicknameSheet(route: AccountNicknameRoute, viewModel: AccountNicknameViewModel = koinViewModel()) {
    val stack = LocalBackStack.current
    var name by rememberSaveable { mutableStateOf(route.nickname) }
    var saving by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("修改昵称", style = MaterialTheme.typography.titleLarge)
            Text(route.userId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PomTextField(name, { if (it.length <= 24) name = it }, Modifier.fillMaxWidth(), label = "昵称",
                placeholder = "填写昵称", supportingText = "留空恢复默认名称 · ${name.length}/24", enabled = !saving)
            Button(onClick = {
                saving = true
                viewModel.save(route, name) { failure ->
                    saving = false
                    if (failure == null) stack.removeLastOrNull()
                    else scope.launch { snackbar.showSnackbar(failure.message ?: "昵称保存失败，请重试。") }
                }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
