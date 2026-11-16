package com.chow.pomegranate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/** 外置标签、主题填充与聚焦描边，保留 Material 的选择、辅助功能与键盘行为。 */
@Composable
fun PomTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String = "", enabled: Boolean = true, supportingText: String? = null,
    isError: Boolean = false, singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(if (focused || isError) 1.5.dp else 0.5.dp,
                when { isError -> MaterialTheme.colorScheme.error; focused -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.outlineVariant })) {
            TextField(value, onValueChange, Modifier.fillMaxWidth().semantics { if (label != null) contentDescription = label }, enabled = enabled,
                placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) }, textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = singleLine, isError = isError, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
                keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, visualTransformation = visualTransformation,
                interactionSource = interactions, colors = pomFieldColors())
        }
        if (supportingText != null) Text(supportingText, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomStateTextField(
    state: TextFieldState, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "",
    enabled: Boolean = true, inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    secure: Boolean = false, visible: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(if (focused) 1.5.dp else .5.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
            // 验证码等宽内容单独布局，不叠加 Material 图标槽位的内边距。
            Row(if (leadingContent == null) Modifier else Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 4.dp)) {
                val inputModifier = if (leadingContent == null) Modifier.fillMaxWidth() else Modifier.weight(1f)
                val inputPadding = if (leadingContent == null) TextFieldDefaults.contentPaddingWithoutLabel() else PaddingValues(12.dp)
                leadingContent?.invoke()
                if (secure) SecureTextField(state, inputModifier.semantics { if (label != null) contentDescription = label }, enabled = enabled, placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    textStyle = MaterialTheme.typography.bodyLarge, inputTransformation = inputTransformation,
                    textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
                    keyboardOptions = keyboardOptions, onKeyboardAction = onKeyboardAction,
                    leadingIcon = leadingIcon, trailingIcon = trailingIcon, interactionSource = interactions, colors = pomFieldColors(), contentPadding = inputPadding)
                else TextField(state, inputModifier.semantics { if (label != null) contentDescription = label }, enabled = enabled, placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    textStyle = MaterialTheme.typography.bodyLarge, inputTransformation = inputTransformation, outputTransformation = outputTransformation,
                    lineLimits = TextFieldLineLimits.SingleLine, keyboardOptions = keyboardOptions, onKeyboardAction = onKeyboardAction,
                    leadingIcon = leadingIcon, trailingIcon = trailingIcon, interactionSource = interactions, colors = pomFieldColors(), contentPadding = inputPadding)
            }
        }
    }
}

@Composable
private fun pomFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, errorContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent,
)
