package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Serializable
data class SavedSchoolTimetable(val timetable: CourseTimetable, val savedAt: Long)

interface SchoolTimetableStorage {
    suspend fun read(owner: String): Map<String, SavedSchoolTimetable>
    suspend fun write(owner: String, values: Map<String, SavedSchoolTimetable>)
}

@Single(binds = [SchoolTimetableStorage::class])
class PreferenceSchoolTimetableStorage(private val preferences: AppPreferenceRepository) : SchoolTimetableStorage {
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun read(owner: String): Map<String, SavedSchoolTimetable> {
        val saved = preferences.get("school_timetables:$owner") ?: return emptyMap()
        return withContext(Dispatchers.Default) { runCatching { json.decodeFromString<Map<String, SavedSchoolTimetable>>(saved) }.getOrDefault(emptyMap()) }
    }
    override suspend fun write(owner: String, values: Map<String, SavedSchoolTimetable>) {
        val text = withContext(Dispatchers.Default) { json.encodeToString(values) }
        preferences.set("school_timetables:$owner", text)
    }
}

/** 每个账号、每个学期首次进入获取一次；显式刷新才再次请求，原始数据单独保存。 */
@Single
class SchoolTimetableRepository(private val academic: AcademicAffairs, private val storage: SchoolTimetableStorage,
    private val session: SessionManager) {
    private val mutex = Mutex()
    private val fresh = mutableMapOf<Pair<String, Semester>, SavedSchoolTimetable>()
    private val saved = MutableStateFlow<Map<String, List<SavedSchoolTimetable>>>(emptyMap())
    val savedByOwner: StateFlow<Map<String, List<SavedSchoolTimetable>>> = saved
    fun owner(): String = Json.encodeToString(listOf(session.userId.value.orEmpty(),
        if (session.accounts.value.any { it.isActive && it.isDemo }) "demo" else "academic"))
    private fun publish(owner: String, values: Map<String, SavedSchoolTimetable>) {
        saved.value = saved.value + (owner to values.values.sortedByDescending { it.timetable.semester })
    }
    suspend fun readSaved(): List<SavedSchoolTimetable> = mutex.withLock {
        val owner = owner()
        val values = storage.read(owner)
        publish(owner, values)
        values.values.sortedByDescending { it.timetable.semester }
    }
    suspend fun get(semester: Semester, refresh: Boolean = false): CourseTimetable = mutex.withLock {
        val owner = owner()
        val key = owner to semester
        if (!refresh) fresh[key]?.let { return@withLock it.timetable }
        val savedValues = storage.read(owner)
        publish(owner, savedValues)
        val data = try { academic.enrollment.getCourseTimetable(semester) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { return@withLock savedValues[semester.toString()]?.timetable ?: throw error }
        check(owner == owner()) { "账号已切换，请重试。" }
        val entry = SavedSchoolTimetable(data, Clock.System.now().toEpochMilliseconds())
        val values = savedValues + (semester.toString() to entry)
        storage.write(owner, values)
        fresh[key] = entry
        publish(owner, values)
        data
    }
    suspend fun delete(owner: String, semester: Semester) = mutex.withLock {
        check(owner == owner()) { "账号已切换，请重新打开本地数据。" }
        val values = storage.read(owner) - semester.toString()
        storage.write(owner, values)
        fresh.remove(owner to semester)
        publish(owner, values)
    }
    suspend fun semesters(selected: Semester): List<Semester> {
        val remote = academic.enrollment.getCourseTimetableSemesters()
        check(remote.isNotEmpty()) { "暂时无法读取学期列表。" }
        val stored = readSaved().map { it.timetable.semester }
        return (remote + stored + selected + Semester.now()).distinct().sortedDescending()
    }
}
