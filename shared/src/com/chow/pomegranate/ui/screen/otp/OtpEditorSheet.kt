package com.chow.pomegranate.ui.screen.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.material.icons.twotone.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import com.chow.pomegranate.ui.component.PomTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.ResultEffect
import com.chow.pomegranate.tool.clipboard.readPlainText
import com.chow.pomegranate.clipboard_read_failed
import com.chow.pomegranate.Res
import com.chow.pomegranate.close
import com.chow.pomegranate.data.local.OtpAccountEntity
import com.chow.pomegranate.otp_account
import com.chow.pomegranate.otp_account_missing
import com.chow.pomegranate.otp_add_account
import com.chow.pomegranate.otp_advanced
import com.chow.pomegranate.otp_defaults
import com.chow.pomegranate.otp_digits
import com.chow.pomegranate.otp_digits_invalid
import com.chow.pomegranate.otp_edit_account
import com.chow.pomegranate.otp_editor_hint
import com.chow.pomegranate.otp_import_algorithm
import com.chow.pomegranate.otp_import_empty
import com.chow.pomegranate.otp_issuer
import com.chow.pomegranate.otp_loading
import com.chow.pomegranate.otp_paste
import com.chow.pomegranate.otp_period
import com.chow.pomegranate.otp_period_invalid
import com.chow.pomegranate.otp_required
import com.chow.pomegranate.otp_save_failed
import com.chow.pomegranate.otp_saving
import com.chow.pomegranate.otp_scan
import com.chow.pomegranate.otp_secret
import com.chow.pomegranate.otp_secret_hint
import com.chow.pomegranate.otp_secret_invalid
import com.chow.pomegranate.repository.OtpAccountRepository
import com.chow.pomegranate.save
import com.chow.pomegranate.tool.otp.OtpAuthAccount
import com.chow.pomegranate.tool.otp.TotpGenerator
import com.chow.pomegranate.tool.otp.parseOtpAuthUri
import com.chow.pomegranate.ui.component.AppSnackbarHost
import com.chow.pomegranate.ui.component.VisibilityIconButton
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Navigation3 的底部弹层内容；导入、表单滚动、固定保存操作各自占位。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OtpEditorSheet(accountId: Long?, repository: OtpAccountRepository = koinInject()) {
    val backStack = LocalBackStack.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val keyboard = LocalSoftwareKeyboardController.current
    var issuer by rememberSaveable(accountId) { mutableStateOf("") }
    var accountName by rememberSaveable(accountId) { mutableStateOf("") }
    var secret by rememberSaveable(accountId) { mutableStateOf("") }
    var digits by rememberSaveable(accountId) { mutableStateOf("6") }
    var period by rememberSaveable(accountId) { mutableStateOf("30") }
    var loaded by rememberSaveable(accountId) { mutableStateOf(accountId == null) }
    var advanced by rememberSaveable(accountId) { mutableStateOf(false) }
    var submitted by rememberSaveable(accountId) { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val importSnackbar = remember { SnackbarHostState() }
    val missingMessage = stringResource(Res.string.otp_account_missing)
    val clipboardReadFailed = stringResource(Res.string.clipboard_read_failed)
    val importEmptyMessage = stringResource(Res.string.otp_import_empty)
    val algorithmMessage = stringResource(Res.string.otp_import_algorithm)
    val saveFailedMessage = stringResource(Res.string.otp_save_failed)
    val requiredMessage = stringResource(Res.string.otp_required)
    val secretValid = remember(secret) { runCatching { TotpGenerator(secret) }.isSuccess }
    val digitsValid = digits.toIntOrNull() in 6..8
    val periodValid = (period.toLongOrNull() ?: 0) > 0
    val valid = issuer.isNotBlank() && accountName.isNotBlank() && secretValid && digitsValid && periodValid
    val rotation by animateFloatAsState(
        if (advanced) 180f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(), label = "OTP advanced options",
    )
    val dismiss = {
        if (backStack.lastOrNull() == OtpEditorRoute(accountId)) backStack.removeLastOrNull()
        Unit
    }
    fun showImportFeedback(feedback: String) {
        scope.launch {
            importSnackbar.currentSnackbarData?.dismiss()
            importSnackbar.showSnackbar(feedback)
        }
    }
    fun applyImportedAccount(account: OtpAuthAccount) {
        issuer = account.issuer
        accountName = account.accountName
        secret = account.secret
        digits = account.digits.toString()
        period = account.periodSeconds.toString()
        submitted = false
        message = null
        importSnackbar.currentSnackbarData?.dismiss()
    }
    fun importFromClipboard(text: String) {
        val account = parseOtpAuthUri(text)
        when {
            account == null -> showImportFeedback(importEmptyMessage)
            account.algorithm != "SHA1" -> showImportFeedback(algorithmMessage)
            else -> applyImportedAccount(account)
        }
    }
    ResultEffect<OtpAuthAccount> { applyImportedAccount(it) }
    LaunchedEffect(accountId) {
        if (accountId != null && !loaded) {
            runCatching { repository.get(accountId) }.onSuccess { account ->
                if (account == null) message = missingMessage else {
                    issuer = account.issuer
                    accountName = account.accountName
                    secret = account.secret
                    digits = account.digits.toString()
                    period = account.periodSeconds.toString()
                    loaded = true
                }
            }.onFailure { message = it.message ?: saveFailedMessage }
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (accountId == null) Res.string.otp_add_account else Res.string.otp_edit_account),
                    style = MaterialTheme.typography.headlineSmall)
                if (accountId == null) Text(stringResource(Res.string.otp_editor_hint),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!loaded) {
                Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                    if (message == null) LoadingIndicator(Modifier.size(40.dp))
                    else Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            } else {
                OtpImportActions(
                    enabled = !saving,
                    onScan = {
                        message = null
                        importSnackbar.currentSnackbarData?.dismiss()
                        keyboard?.hide()
                        if (backStack.lastOrNull() == OtpEditorRoute(accountId)) backStack.add(OtpScanRoute)
                    },
                    onPaste = {
                        scope.launch {
                            try { importFromClipboard(clipboard.readPlainText().orEmpty()) }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { showImportFeedback(clipboardReadFailed) }
                        }
                    },
                )
                EditorField(issuer, { issuer = it }, stringResource(Res.string.otp_issuer), enabled = !saving,
                    error = if (submitted && issuer.isBlank()) requiredMessage else null)
                EditorField(accountName, { accountName = it }, stringResource(Res.string.otp_account), enabled = !saving,
                    error = if (submitted && accountName.isBlank()) requiredMessage else null)
                EditorField(
                    secret, { secret = it }, stringResource(Res.string.otp_secret), enabled = !saving,
                    error = if (submitted && !secretValid) stringResource(Res.string.otp_secret_invalid) else null,
                    hint = stringResource(Res.string.otp_secret_hint),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { VisibilityIconButton(secretVisible, { secretVisible = it }) },
                )
                Column(Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { advanced = !advanced }, enabled = !saving,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(Res.string.otp_advanced), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(Res.string.otp_defaults, digits, period), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ExpandMore, null, Modifier.rotate(rotation))
                        }
                    }
                    AnimatedVisibility(advanced) {
                        BoxWithConstraints(Modifier.padding(top = 16.dp)) {
                            val largeFont = LocalDensity.current.fontScale > 1.3f
                            if (maxWidth < 260.dp || largeFont) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    EditorField(digits, { digits = it.filter(Char::isDigit) }, stringResource(Res.string.otp_digits),
                                        enabled = !saving, error = if (submitted && !digitsValid) stringResource(Res.string.otp_digits_invalid) else null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    EditorField(period, { period = it.filter(Char::isDigit) }, stringResource(Res.string.otp_period),
                                        enabled = !saving, error = if (submitted && !periodValid) stringResource(Res.string.otp_period_invalid) else null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    EditorField(digits, { digits = it.filter(Char::isDigit) }, stringResource(Res.string.otp_digits), Modifier.weight(1f),
                                        enabled = !saving, error = if (submitted && !digitsValid) stringResource(Res.string.otp_digits_invalid) else null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    EditorField(period, { period = it.filter(Char::isDigit) }, stringResource(Res.string.otp_period), Modifier.weight(1f),
                                        enabled = !saving, error = if (submitted && !periodValid) stringResource(Res.string.otp_period_invalid) else null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                }
                            }
                        }
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Column {
            AppSnackbarHost(importSnackbar, Modifier.align(Alignment.CenterHorizontally))
            Button(
                shapes = ButtonDefaults.shapes(),
                enabled = loaded && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = {
                    submitted = true
                    if (!digitsValid || !periodValid) advanced = true
                    if (valid) {
                        saving = true
                        message = null
                        keyboard?.hide()
                        scope.launch {
                            runCatching {
                                repository.save(OtpAccountEntity(accountId ?: 0, issuer, accountName, secret,
                                    digits.toInt(), period.toLong()))
                            }.onSuccess { dismiss() }.onFailure { message = it.message ?: saveFailedMessage }
                            saving = false
                        }
                    }
                },
            ) { Text(stringResource(if (saving) Res.string.otp_saving else Res.string.save)) }
        }
    }
}

@Composable
private fun OtpImportActions(enabled: Boolean, onScan: () -> Unit, onPaste: () -> Unit) {
    BoxWithConstraints {
        if (LocalDensity.current.fontScale > 1.3f || maxWidth < 260.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onScan, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.TwoTone.QrCodeScanner, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_scan))
                }
                FilledTonalButton(onPaste, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.TwoTone.ContentPaste, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_paste))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onScan, enabled = enabled, modifier = Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                    Icon(Icons.TwoTone.QrCodeScanner, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_scan))
                }
                FilledTonalButton(onPaste, enabled = enabled, modifier = Modifier.weight(1f), contentPadding = PaddingValues(12.dp)) {
                    Icon(Icons.TwoTone.ContentPaste, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.otp_paste))
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    PomTextField(
        value = value, onValueChange = onValueChange, label = label,
        singleLine = true, modifier = modifier.fillMaxWidth(), enabled = enabled,
        isError = error != null,
        supportingText = error ?: hint,
        keyboardOptions = keyboardOptions, visualTransformation = visualTransformation, trailingIcon = trailingIcon,
    )
}
