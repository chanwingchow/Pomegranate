package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import org.koin.core.annotation.Single
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.chow.pomegranate.session.SessionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 课表数据仓库。 */
@Single
class TimetableRepository(
    private val academicAffairs: AcademicAffairs,
    private val session: SessionManager,
) {
    private data class Owner(val userId: String?, val demo: Boolean)
    private fun owner() = Owner(session.userId.value, session.accounts.value.any { it.isActive && it.isDemo })
    private val mutex = Mutex()
    private val cache = mutableMapOf<Pair<Owner, Semester>, Timetable>()
    val current: StateFlow<Timetable?>
        field = MutableStateFlow<Timetable?>(null)

    /**
     * 返回 [semester] 的课表。
     *
     * @param semester 学期，默认为当前学期
     */
    suspend fun getTimetable(semester: Semester = Semester.now(), refresh: Boolean = false): Timetable = mutex.withLock {
        val owner = owner()
        check(!owner.userId.isNullOrBlank()) { "请先登录。" }
        val key = owner to semester
        val timetable = if (refresh) null else cache[key]
        val result = timetable ?: academicAffairs.enrollment.getTimetable(semester)
        check(owner == owner()) { "账号已切换，请重试。" }
        cache[key] = result
        current.value = result
        result
    }
}
