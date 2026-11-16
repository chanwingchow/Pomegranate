package com.chow.pomegranate.ui.screen.main.me

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chow.pomegranate.ui.component.PomTextField
import com.chow.pomegranate.ui.theme.DEFAULT_PRIMARY_COLOR
import kotlin.math.roundToInt

private val hueColors = (0..6).map { Color.hsv(it * 60f, 1f, 1f) }

/** 颜色草稿只在保存后应用；矩形选色区负责深浅，Material 滑杆负责色相。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeColorSheet(
    selected: Color?, saving: Boolean, onColorChange: (Color) -> Unit,
    onSave: () -> Unit, onReset: () -> Unit,
) {
    val initial = remember { (selected ?: DEFAULT_PRIMARY_COLOR).pickerHsv() }
    var hue by rememberSaveable { mutableFloatStateOf(initial.hue) }
    var saturation by rememberSaveable { mutableFloatStateOf(initial.saturation) }
    var brightness by rememberSaveable { mutableFloatStateOf(initial.brightness) }
    var alpha by rememberSaveable { mutableFloatStateOf(selected?.alpha ?: 1f) }
    var input by rememberSaveable { mutableStateOf((selected ?: DEFAULT_PRIMARY_COLOR).pickerCode()) }
    val color = Color.hsv(hue, saturation, brightness, alpha)
    val valid = input.length in setOf(6, 8) && input.toColor() != null
    fun pick(picked: Color, updateInput: Boolean = true) {
        val hsv = picked.pickerHsv()
        hue = hsv.hue; saturation = hsv.saturation; brightness = hsv.brightness; alpha = picked.alpha
        if (updateInput) input = picked.pickerCode()
        onColorChange(picked)
    }
    fun pickHsv(h: Float, s: Float, v: Float) {
        hue = h; saturation = s; brightness = v
        val picked = Color.hsv(h, s, v, alpha)
        input = picked.pickerCode()
        onColorChange(picked)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("喜欢的颜色", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Surface(Modifier.size(44.dp).semantics { contentDescription = "当前颜色预览" }, shape = MaterialTheme.shapes.medium, color = color,
                border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {}
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SaturationBrightnessPicker(hue, saturation, brightness, enabled = !saving,
                onChange = { s, v -> pickHsv(hue, s, v) })
            Slider(value = hue, onValueChange = { pickHsv(it, saturation, brightness) }, valueRange = 0f..360f,
                enabled = !saving, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "色相" },
                track = {
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(hueColors)))
                })
            PomTextField(input, { value ->
                input = value.removePrefix("#").filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(8).uppercase()
                if (input.length in setOf(6, 8)) input.toColor()?.let { pick(it, updateInput = false) }
            }, label = "颜色值", placeholder = "RRGGBB 或 AARRGGBB", enabled = !saving,
                isError = !valid, supportingText = if (valid) null else "请输入 6 位或 8 位十六进制颜色值",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done))
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val hsv = DEFAULT_PRIMARY_COLOR.pickerHsv()
                hue = hsv.hue; saturation = hsv.saturation; brightness = hsv.brightness; alpha = 1f
                input = DEFAULT_PRIMARY_COLOR.pickerCode(); onReset()
            }, enabled = !saving) { Text("恢复默认") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSave, enabled = !saving && valid) { Text(if (saving) "保存中" else "保存") }
        }
    }
}

private fun Color.pickerCode(): String = toHexString().uppercase().let { if (alpha == 1f) it.takeLast(6) else it }

@Composable
private fun SaturationBrightnessPicker(hue: Float, saturation: Float, brightness: Float, enabled: Boolean, onChange: (Float, Float) -> Unit) {
    val currentOnChange by rememberUpdatedState(onChange)
    val hueColor = Color.hsv(hue, 1f, 1f)
    Canvas(Modifier.fillMaxWidth().height(168.dp).clip(MaterialTheme.shapes.medium)
        .semantics {
            contentDescription = "颜色深浅"
            stateDescription = "鲜艳程度 ${(saturation * 100).roundToInt()}%，明亮程度 ${(brightness * 100).roundToInt()}%"
            if (enabled) customActions = listOf(
                CustomAccessibilityAction("更鲜艳") { onChange((saturation + .05f).coerceAtMost(1f), brightness); true },
                CustomAccessibilityAction("更柔和") { onChange((saturation - .05f).coerceAtLeast(0f), brightness); true },
                CustomAccessibilityAction("更明亮") { onChange(saturation, (brightness + .05f).coerceAtMost(1f)); true },
                CustomAccessibilityAction("更深暗") { onChange(saturation, (brightness - .05f).coerceAtLeast(0f)); true },
            )
        }
        .onKeyEvent { event ->
            if (!enabled || event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.DirectionRight -> { onChange((saturation + .02f).coerceAtMost(1f), brightness); true }
                Key.DirectionLeft -> { onChange((saturation - .02f).coerceAtLeast(0f), brightness); true }
                Key.DirectionUp -> { onChange(saturation, (brightness + .02f).coerceAtMost(1f)); true }
                Key.DirectionDown -> { onChange(saturation, (brightness - .02f).coerceAtLeast(0f)); true }
                else -> false
            }
        }.focusable(enabled)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val inset = 12.dp.toPx()
            fun pickAt(position: Offset) {
                val (s, v) = colorPoint(position.x, position.y, size.width.toFloat(), size.height.toFloat(), inset)
                currentOnChange(s, v)
            }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume(); pickAt(down.position)
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull { it.id == down.id }?.let { change -> change.consume(); pickAt(change.position) }
                } while (event.changes.any { it.pressed })
            }
        }) {
        val inset = 12.dp.toPx()
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor), startX = inset, endX = size.width - inset))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = inset, endY = size.height - inset))
        val point = Offset(inset + saturation * (size.width - 2 * inset), inset + (1f - brightness) * (size.height - 2 * inset))
        drawCircle(Color.Black.copy(alpha = .6f), 9.dp.toPx(), point, style = Stroke(2.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), point, style = Stroke(2.dp.toPx()))
    }
}
