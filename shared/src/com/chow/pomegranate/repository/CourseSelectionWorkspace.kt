package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.ui.component.StudyDetailRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Serializable
data class FavoriteCourse(val course: SelectableCourse, val category: String)
@Serializable
data class CourseFavoriteScope(val key: String, val semester: String, val roundId: String, val roundName: String, val endTime: String) {
    fun isExpired(): Boolean = LocalDateTime.parse(endTime) <= Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Shanghai"))
}
data class ArchivedCourseFavorite(val scope: CourseFavoriteScope, val favorite: FavoriteCourse)
@Serializable
data class CourseCartItem(val id: String, val courseCode: String, val name: String, val teacher: String,
    val credits: Double, val category: String, val detail: StudyDetailRoute, val priority: Int? = null)
@Serializable
enum class CourseBatchOperation { Select, Drop }
data class CourseItemResult(val item: CourseCartItem, val success: Boolean, val message: String)
data class CourseBatchReport(val operation: CourseBatchOperation, val results: List<CourseItemResult>, val preference: Boolean) {
    val successCount get() = results.count { it.success }
    val message get() = "${if (preference && operation == CourseBatchOperation.Select) "志愿提交" else if (operation == CourseBatchOperation.Drop) "退课" else "选课"}：成功 $successCount 门，失败 ${results.size - successCount} 门"
}
data class CourseWorkspaceState(
    val expiryMinute: Long = 0, val key: String = "", val scope: CourseFavoriteScope? = null, val expired: Boolean = false,
    val archived: List<ArchivedCourseFavorite> = emptyList(), val favorites: List<FavoriteCourse> = emptyList(),
    val pendingSelect: List<CourseCartItem> = emptyList(), val pendingDrop: List<CourseCartItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(), val selectionOverrides: Map<String, Boolean> = emptyMap(), val error: String? = null, val submittedPreferences: Map<String, Int> = emptyMap(),
    val priorities: Map<String, Int> = emptyMap(), val preferenceMode: Boolean = false, val running: Boolean = false, val completed: Int = 0, val total: Int = 0,
    val currentName: String = "", val report: CourseBatchReport? = null,
)

/** 收藏按账号、演示/真实数据及轮次隔离；待选/待退只保留于当前轮次，批量请求串行执行。 */
@Single
class CourseSelectionWorkspace(
    private val preferences: AppPreferenceRepository,
    private val session: SessionManager,
    private val mode: DemoModeRepository,
    private val courses: CourseSelectionRepository,
) {
    val state: StateFlow<CourseWorkspaceState>
        field = MutableStateFlow(CourseWorkspaceState())
    private val storage = Mutex()
    private val operation = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var activeIdentity = ""
    private fun identity() = "${mode.enabled.value}:${session.userId.value}"
    private fun preferenceKey(key: String) = "course_favorites:$key"

    fun invalidate() {
        state.value = CourseWorkspaceState(running = state.value.running)
        activeIdentity = ""
    }

    private fun registryKey(owner: String) = "course_favorite_scopes:$owner"
    private suspend fun readScopes(owner: String): List<CourseFavoriteScope> = preferences.get(registryKey(owner))?.let { json.decodeFromString<List<CourseFavoriteScope>>(it) }.orEmpty()

    suspend fun activate(overview: CourseSelectionOverview) = storage.withLock {
        val owner = identity()
        val term = overview.entrance.semester.toString()
        val key = json.encodeToString(listOf(owner, term, overview.entrance.id))
        val scope = CourseFavoriteScope(key, term, overview.entrance.id, overview.entrance.name, overview.entrance.endTime.toString())
        var raw = preferences.get(preferenceKey(key))
        if (raw == null) {
            // 兼容本次开发早期仅按轮次保存的收藏，不混入其他账号或数据模式。
            val legacy = json.encodeToString(listOf(owner, overview.entrance.id))
            raw = preferences.get(preferenceKey(legacy))
            if (raw != null) {
                preferences.set(preferenceKey(key), raw)
                preferences.remove(preferenceKey(legacy))
                preferences.get("course_preference_mode:$legacy")?.let { preferences.set("course_preference_mode:$key", it) }
            }
        }
        val scopes = readScopes(owner).filterNot { it.key == key } + scope
        preferences.set(registryKey(owner), json.encodeToString(scopes))
        val archived = buildList {
            scopes.filterNot { it.key == key }.forEach { old ->
                preferences.get(preferenceKey(old.key))?.let { saved ->
                    json.decodeFromString<List<FavoriteCourse>>(saved).forEach { add(ArchivedCourseFavorite(old, it)) }
                }
            }
        }
        if (owner != identity()) return@withLock
        if (state.value.key != key) {
            activeIdentity = owner
            state.value = CourseWorkspaceState(key = key, scope = scope, expired = scope.isExpired(), archived = archived,
                favorites = raw?.let { json.decodeFromString<List<FavoriteCourse>>(it) }.orEmpty(), running = state.value.running,
                preferenceMode = preferences.get("course_preference_mode:$key") == "true")
        }
        val ids = overview.selectedCourses.map { it.id }.toSet()
        state.update { it.copy(scope = scope, expired = scope.isExpired(), archived = archived, selectedIds = ids,
            selectionOverrides = emptyMap(), pendingSelect = it.pendingSelect.filterNot { c -> c.id in ids }) }
    }

    fun refreshExpiry() { state.update { it.copy(expired = it.scope?.isExpired() == true, expiryMinute = Clock.System.now().epochSeconds / 60) } }

    /** 仅由用户点击触发；到期本身只改标记，不删除收藏。 */
    suspend fun clearExpiredFavorites(): Int = storage.withLock {
        val owner = activeIdentity
        check(owner == identity() && state.value.key.isNotEmpty()) { "账号已切换，请重新打开收藏。" }
        val scopes = readScopes(owner)
        val expired = scopes.filter { it.isExpired() }
        var removed = 0
        expired.forEach { old ->
            preferences.get(preferenceKey(old.key))?.let { removed += json.decodeFromString<List<FavoriteCourse>>(it).size }
            preferences.remove(preferenceKey(old.key))
        }
        preferences.set(registryKey(owner), json.encodeToString(scopes - expired.toSet()))
        val keys = expired.map { it.key }.toSet()
        state.update { if (activeIdentity != owner || owner != identity()) it else it.copy(
            favorites = if (it.key in keys) emptyList() else it.favorites,
            archived = it.archived.filterNot { old -> old.scope.key in keys }) }
        removed
    }

    suspend fun toggleFavorite(course: SelectableCourse, category: String) = storage.withLock {
        val current = state.value
        check(current.key.isNotEmpty() && activeIdentity == identity()) { "请先重新加载选课列表。" }
        val next = if (current.favorites.any { it.course.id == course.id }) current.favorites.filterNot { it.course.id == course.id }
            else current.favorites + FavoriteCourse(course, category)
        preferences.set(preferenceKey(current.key), json.encodeToString(next))
        state.update { if (it.key == current.key) it.copy(favorites = next) else it }
    }

    suspend fun refreshFavorites(items: List<SelectableCourse>) = storage.withLock {
        val current = state.value
        if (activeIdentity != identity() || current.favorites.isEmpty()) return@withLock
        val fresh = items.associateBy { it.id }
        val next = current.favorites.map { favorite -> fresh[favorite.course.id]?.let { favorite.copy(course = it) } ?: favorite }
        if (next != current.favorites) {
            preferences.set(preferenceKey(current.key), json.encodeToString(next))
            state.update { if (it.key == current.key) it.copy(favorites = next) else it }
        }
    }

    fun showError(message: String?) { state.update { it.copy(error = message) } }

    suspend fun setPreferenceMode(enabled: Boolean) = storage.withLock {
        if (state.value.running) return@withLock
        val key = state.value.key
        check(key.isNotEmpty() && activeIdentity == identity()) { "请先刷新选课列表。" }
        preferences.set("course_preference_mode:$key", enabled.toString())
        state.update { it.copy(preferenceMode = enabled, error = null, pendingSelect = if (enabled) it.pendingSelect.mapIndexed { i, c ->
            c.copy(priority = (it.pendingSelect.take(i).count { previous -> previous.courseCode == c.courseCode } + 1).takeIf { n -> n <= 3 })
        } else it.pendingSelect.map { c -> c.copy(priority = null) }) }
    }

    fun togglePending(item: CourseCartItem, drop: Boolean) {
        if (state.value.running) return
        state.update { current ->
            val list = if (drop) current.pendingDrop else current.pendingSelect
            val next = if (list.any { it.id == item.id }) list.filterNot { it.id == item.id } else list + item.copy(
                priority = if (!drop && current.preferenceMode) item.priority ?: suggestedPriority(item) else null)
            if (drop) current.copy(pendingDrop = next) else current.copy(pendingSelect = next)
        }
    }
    fun removePending(id: String, drop: Boolean) {
        if (state.value.running) return
        state.update { if (drop) it.copy(pendingDrop = it.pendingDrop.filterNot { c -> c.id == id })
            else it.copy(pendingSelect = it.pendingSelect.filterNot { c -> c.id == id }) }
    }
    fun clearPending(drop: Boolean) {
        if (state.value.running) return
        state.update { if (drop) it.copy(pendingDrop = emptyList()) else it.copy(pendingSelect = emptyList()) }
    }
    fun setPriority(id: String, priority: Int) {
        if (state.value.running || priority !in 1..3) return
        state.update { current -> current.copy(priorities = current.priorities + (id to priority), pendingSelect = current.pendingSelect.map { if (it.id == id) it.copy(priority = priority) else it }) }
    }

    fun suggestedPriority(item: CourseCartItem): Int? {
        val current = state.value
        return current.priorities[item.id] ?: current.pendingSelect.firstOrNull { it.id == item.id }?.priority ?: (1..3).firstOrNull { p ->
            (current.pendingSelect.filter { it.courseCode == item.courseCode && it.id != item.id }.map { it.priority } +
                current.submittedPreferences.filterKeys { it.startsWith("${item.courseCode}:") && it != "${item.courseCode}:${item.id}" }.values).none { it == p }
        }
    }

    /** 每门课保留独立结果；成功项移出清单，失败项原位保留。相同教学班不重复发送。 */
    suspend fun submit(items: List<CourseCartItem>, drop: Boolean, expectedOwner: String = state.value.key): CourseBatchReport? {
        if (items.isEmpty() || !operation.tryLock()) return null
        val snapshot = state.value
        val owner = activeIdentity
        val preference = snapshot.preferenceMode && !drop
        val unique = items.distinctBy { it.id }
        val results = mutableListOf<CourseItemResult>()
        try {
            check(snapshot.key == expectedOwner && snapshot.key.isNotEmpty() && owner == identity()) { "账号或选课轮次已变化，请重新加载后操作。" }
            state.update { it.copy(error = null) }
            check(snapshot.scope?.isExpired() != true) { "本轮选课已结束，不能使用旧教学班继续操作。" }
            if (preference) validatePriorities(unique, snapshot.submittedPreferences)
            state.update { it.copy(running = true, completed = 0, total = unique.size, report = null) }
            unique.forEach { item ->
                if (owner != identity() || state.value.key != snapshot.key || snapshot.scope?.isExpired() == true) {
                    results.add(CourseItemResult(item, false, "账号或轮次已切换，未提交本课程。"))
                } else {
                    state.update { it.copy(currentName = item.name) }
                    val result = try {
                        if (drop) courses.drop(item.id) else courses.select(item.id, CourseSelectCategory.valueOf(item.category),
                            if (preference) CourseSelectPriority.entries.first { it.id == item.priority } else null)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { CourseOperateResult(false, error.message ?: "请求失败，请刷新后重试。") }
                    results.add(CourseItemResult(item, result.isSuccess, result.message ?: if (result.isSuccess) "提交成功" else "提交失败"))
                    if (result.isSuccess) state.update { current ->
                        if (current.key != snapshot.key) current else current.copy(
                            selectionOverrides = current.selectionOverrides + (item.id to !drop),
                            pendingSelect = current.pendingSelect.filterNot { it.id == item.id }, pendingDrop = current.pendingDrop.filterNot { it.id == item.id },
                            selectedIds = if (preference) current.selectedIds else if (drop) current.selectedIds - item.id else current.selectedIds + item.id,
                            submittedPreferences = if (preference) current.submittedPreferences + ("${item.courseCode}:${item.id}" to requireNotNull(item.priority))
                                else if (drop) current.submittedPreferences.filterKeys { !it.endsWith(":${item.id}") } else current.submittedPreferences)
                    }
                }
                if (state.value.key == snapshot.key) state.update { it.copy(completed = results.size) }
            }
            val report = CourseBatchReport(if (drop) CourseBatchOperation.Drop else CourseBatchOperation.Select, results, preference)
            if (state.value.key == snapshot.key) {
                state.update { it.copy(report = report) }
                if (report.successCount > 0) courses.notifyOperation(drop)
            }
            return report
        } finally {
            state.update { it.copy(running = false, currentName = "") }
            operation.unlock()
        }
    }
}

internal fun validatePriorities(items: List<CourseCartItem>, submitted: Map<String, Int> = emptyMap()) {
    items.groupBy { it.courseCode }.forEach { (code, group) ->
        val existing = submitted.filterKeys { it.startsWith("$code:") && group.none { item -> it == "$code:${item.id}" } }.values
        require(group.size + existing.size <= 3) { "${group.first().name} 每门课程最多填报三个教学班志愿。" }
        require(group.all { it.priority in 1..3 }) { "请为每个教学班设置第一、第二或第三志愿。" }
        require((group.map { it.priority } + existing).distinct().size == group.size + existing.size) { "${group.first().name} 的志愿顺序重复，请调整。" }
    }
}
