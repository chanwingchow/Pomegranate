package com.chow.pomegranate.ui.screen.main.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.chow.pomegranate.ui.navigation.LocalBackStack
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.time.Instant

@Serializable data class TimetableWeekRoute(val week: Int) : NavKey
internal data class TimetableWeekSelection(val week: Int)
@Serializable data class TimetableDateRoute(val initialDate: Long, val firstDate: Long, val lastDate: Long) : NavKey
internal data class TimetableDateSelection(val date: LocalDate)

@Composable
internal fun TimetableWeekSheet(route: TimetableWeekRoute) {
    val backStack = LocalBackStack.current
    val results = LocalResultEventBus.current
    var draft by rememberSaveable { mutableIntStateOf(route.week.coerceIn(0, TIMETABLE_MAX_WEEK - 1)) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("选择查看的周次", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { draft-- }, enabled = draft > 0) { Icon(Icons.Rounded.Remove, "上一周") }
            Text("第 $draft 周", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            FilledTonalIconButton(onClick = { draft++ }, enabled = draft < TIMETABLE_MAX_WEEK - 1) { Icon(Icons.Rounded.Add, "下一周") }
        }
        Column {
            Slider(draft.toFloat(), onValueChange = { draft = it.roundToInt() }, valueRange = 0f..(TIMETABLE_MAX_WEEK - 1).toFloat(),
                steps = TIMETABLE_MAX_WEEK - 2, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "查看周次" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0 周", style = MaterialTheme.typography.labelMedium)
                Text("${TIMETABLE_MAX_WEEK - 1} 周", style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("只切换查看的周次，当前周次保持不变。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { results.sendResult(TimetableWeekSelection(draft)); backStack.removeLastOrNull() }, modifier = Modifier.fillMaxWidth()) {
            Text("应用")
        }
    }
}

/** 参考 AdMobPulse 的紧凑月历，七列按可用宽度等分，避免原生固定日格在窄屏被裁切。 */
@Composable
internal fun TimetableDateDialog(route: TimetableDateRoute) {
    val backStack = LocalBackStack.current
    val results = LocalResultEventBus.current
    var selectedMillis by rememberSaveable { mutableLongStateOf(route.initialDate) }
    val initial = utcDate(route.initialDate)
    var monthMillis by rememberSaveable { mutableLongStateOf(LocalDate(initial.year, initial.month.number, 1).utcMillis()) }
    val month = utcDate(monthMillis)
    val first = utcDate(route.firstDate)
    val last = utcDate(route.lastDate)
    val window = LocalWindowInfo.current.containerSize
    val width = with(LocalDensity.current) { minOf(window.width.toDp() * .86f, 336.dp) }
    val maximumHeight = with(LocalDensity.current) { window.height.toDp() * .9f }
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.width(width), shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerHigh) {
        Column(Modifier.heightIn(max = maximumHeight).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("选择日期", style = MaterialTheme.typography.titleMedium)
            val chosen = utcDate(selectedMillis)
            Text("${chosen.year}年${chosen.month.number}月${chosen.day}日", style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthMillis = month.minus(DatePeriod(months = 1)).utcMillis() },
                    enabled = month > LocalDate(first.year, first.month.number, 1), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "上个月", Modifier.size(22.dp))
                }
                Text("${month.year}年${month.month.number}月", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center)
                IconButton(onClick = { monthMillis = month.plus(DatePeriod(months = 1)).utcMillis() },
                    enabled = month < LocalDate(last.year, last.month.number, 1), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "下个月", Modifier.size(22.dp))
                }
            }
            Column {
                Row(Modifier.fillMaxWidth()) {
                    "一二三四五六日".forEach { label -> Text(label.toString(), Modifier.weight(1f).padding(bottom = 6.dp),
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = colors.onSurfaceVariant) }
                }
                val cells = remember(month) { calendarMonthCells(month) }
                cells.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).height(40.dp))
                            else {
                                val allowed = date in first..last
                                val chosenDay = date == chosen
                                BoxWithConstraints(Modifier.weight(1f).height(40.dp).clip(CircleShape)
                                    .clickable(enabled = allowed, role = Role.Button) { selectedMillis = date.utcMillis() }
                                    .semantics { contentDescription = "${date.year}年${date.month.number}月${date.day}日"; selected = chosenDay },
                                    contentAlignment = Alignment.Center) {
                                    val diameter = minOf(32.dp, maxWidth - 2.dp)
                                    if (chosenDay) Box(Modifier.size(diameter).background(colors.primary, CircleShape))
                                    Text(date.day.toString(), maxLines = 1,
                                        style = if (LocalDensity.current.fontScale > 1.3f) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                                        color = when { chosenDay -> colors.onPrimary; !allowed -> colors.onSurface.copy(alpha = .38f); else -> colors.onSurface })
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = { backStack.removeLastOrNull() }) { Text("取消") }
                Button(onClick = { results.sendResult(TimetableDateSelection(chosen)); backStack.removeLastOrNull() }) { Text("查看") }
            }
        }
    }
}

internal fun calendarMonthCells(month: LocalDate): List<LocalDate?> {
    val first = LocalDate(month.year, month.month.number, 1)
    val days = first.daysUntil(first.plus(DatePeriod(months = 1)))
    val offset = first.dayOfWeek.ordinal
    return List(42) { index -> (index - offset + 1).takeIf { it in 1..days }?.let { LocalDate(first.year, first.month.number, it) } }
}

internal fun LocalDate.utcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
internal fun utcDate(millis: Long): LocalDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
internal fun weekForDate(date: LocalDate, currentWeek: Int, today: LocalDate): Int =
    weekDates(0, today, currentWeek).first().daysUntil(date) / 7

/** 日分页使用同一周历锚点，跨周时按连续日期推进。 */
internal fun timetableDateForDay(index: Int, currentWeek: Int, today: LocalDate): LocalDate =
    weekDates(0, today, currentWeek).first().plus(DatePeriod(days = index))

internal fun dayIndexForDate(date: LocalDate, currentWeek: Int, today: LocalDate): Int =
    weekDates(0, today, currentWeek).first().daysUntil(date)
