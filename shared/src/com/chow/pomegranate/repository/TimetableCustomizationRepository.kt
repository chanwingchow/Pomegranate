package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.academic.affairs.model.TimetableCourse
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/** 只保存用户明确修改过的字段，教务刷新始终保留为独立的原始数据。 */
@Serializable
data class TimetableCourseCustomization(val classroom: String? = null, val note: String = "", val name: String? = null)

/** 可恢复的课程场次引用；教师、教室和周次不参与标识，刷新这些字段不会丢失本地修改。 */
@Serializable
data class TimetableLesson(
    val owner: String, val key: String, val name: String, val teacher: String?,
    val classroom: String?, val weeks: String, val day: Int, val sections: List<Int>, val clazz: String?,
)

internal fun Timetable.ownerKey(): String = Json.encodeToString(listOf(userId, semester.toString()))
internal fun TimetableCourse.lesson(owner: String) = TimetableLesson(owner,
    Json.encodeToString(listOf(name.trim(), dayOfWeek.ordinal.toString(), sections.sorted().joinToString(","), clazz.orEmpty().trim())),
    name, teacher, classroom, weeksString, dayOfWeek.ordinal, sections.sorted(), clazz)

@Single
class TimetableCustomizationRepository(private val preferences: AppPreferenceRepository) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private fun storageKey(owner: String) = "timetable_customizations:$owner"
    private fun decode(value: String?): Map<String, TimetableCourseCustomization> =
        if (value == null) emptyMap() else json.decodeFromString(value)

    fun observe(owner: String) = preferences.observe(storageKey(owner)).map(::decode)
    suspend fun get(owner: String) = decode(preferences.get(storageKey(owner)))

    suspend fun save(lesson: TimetableLesson, classroom: String, note: String, name: String = lesson.name) = mutex.withLock {
        require(name.isNotBlank()) { "课程名称不能为空。" }
        // 只写覆盖层。仅添加备注时不锁定原来的教室，让未修改的教室继续跟随教务。
        val room = classroom.trim().takeUnless { it == lesson.classroom.orEmpty().trim() }
        val title = name.trim().takeUnless { it == lesson.name.trim() }
        val value = TimetableCourseCustomization(room, note.trim(), title)
        val entries = get(lesson.owner).toMutableMap()
        if (value == TimetableCourseCustomization()) entries.remove(lesson.key) else entries[lesson.key] = value
        preferences.set(storageKey(lesson.owner), entries.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) })
    }
}
