package com.chow.pomegranate.repository

import com.chow.pomegranate.data.local.PreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock

internal const val MAX_ACADEMIC_WEEK = 20
internal fun currentLocalDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

@Serializable
internal data class AcademicWeekAnchor(val week: Int, val monday: LocalDate) {
    fun weekOn(date: LocalDate): Int {
        val currentMonday = date.minus(DatePeriod(days = date.dayOfWeek.ordinal))
        return (week + monday.daysUntil(currentMonday) / 7).coerceIn(0, MAX_ACADEMIC_WEEK)
    }
}

/** 保存校准周次及所在周的周一；浏览其他周不改变这个基准。 */
@Single
class AcademicWeekRepository(private val preferences: AppPreferenceRepository) {
    private val mutex = Mutex()
    private fun decode(value: String?): AcademicWeekAnchor? = value?.let {
        runCatching { Json.decodeFromString<AcademicWeekAnchor>(it) }.getOrNull()
    }
    private suspend fun load(date: LocalDate): AcademicWeekAnchor {
        decode(preferences.get(PreferenceKeys.WEEK_ANCHOR))?.let { return it }
        // 旧版本只有数字，首次升级以当周为基准，保留用户已有设置。
        val legacy = preferences.get(PreferenceKeys.WEEK)?.toIntOrNull() ?: 0
        return anchor(legacy, date).also { preferences.set(PreferenceKeys.WEEK_ANCHOR, Json.encodeToString(it)) }
    }
    private fun anchor(week: Int, date: LocalDate) = AcademicWeekAnchor(
        week.coerceIn(0, MAX_ACADEMIC_WEEK), date.minus(DatePeriod(days = date.dayOfWeek.ordinal)),
    )

    suspend fun get(date: LocalDate = currentLocalDate()): Int = mutex.withLock { load(date).weekOn(date) }

    suspend fun set(week: Int, date: LocalDate = currentLocalDate()) = mutex.withLock {
        val value = anchor(week, date)
        preferences.set(PreferenceKeys.WEEK, value.week.toString())
        preferences.set(PreferenceKeys.WEEK_ANCHOR, Json.encodeToString(value))
    }

    fun observe(dates: Flow<LocalDate>): Flow<Int> = combine(
        preferences.observe(PreferenceKeys.WEEK_ANCHOR).onStart { get() }, dates,
    ) { saved, date -> decode(saved)?.weekOn(date) ?: 0 }.distinctUntilChanged()
}
