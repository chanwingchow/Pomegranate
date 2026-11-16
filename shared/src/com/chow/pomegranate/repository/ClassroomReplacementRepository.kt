package com.chow.pomegranate.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Serializable
enum class NameReplacementTarget(val label: String) { Classroom("教室"), Course("课程名称") }

@Serializable
data class ClassroomReplacementRule(val id: String, val source: String, val replacement: String, val enabled: Boolean = true,
    val campus: String? = null, val sourceUrl: String? = null, val target: NameReplacementTarget = NameReplacementTarget.Classroom)

/** 只处理传入的地点文字；最长原文优先，每段原文只替换一次，不级联、不执行正则。 */
class ClassroomReplacements(rules: List<ClassroomReplacementRule> = emptyList(), target: NameReplacementTarget = NameReplacementTarget.Classroom) {
    private val active = rules.filter { it.enabled && it.source.isNotEmpty() && it.target == target }
        .sortedWith(compareBy<ClassroomReplacementRule> { it.isBuiltin }.thenByDescending { it.source.length })

    fun format(value: String, custom: String? = null): String {
        if (custom != null) return custom
        if (active.isEmpty()) return value
        return buildString {
            var position = 0
            while (position < value.length) {
                val rule = active.firstOrNull { value.startsWith(it.source, position) }
                if (rule == null) append(value[position++])
                else {
                    append(rule.replacement)
                    position += rule.source.length
                    // 官方页面及部分系统会同时写正式名和简称，内置规则合并重复的括注。
                    if (rule.sourceUrl != null) {
                        val digitAlias = rule.replacement.replace("北二", "北2").replace("北三", "北3").replace("北五", "北5")
                        val suffix = listOf(rule.replacement, digitAlias).distinct().flatMap { listOf("（$it）", "($it)") }
                            .firstOrNull { value.startsWith(it, position) }
                        if (suffix != null) position += suffix.length
                    }
                }
            }
        }
    }

    fun matches(value: String, query: String): Boolean {
        fun nickname(text: String) = text.replace("北2", "北二").replace("北3", "北三")
            .replace("北4", "北四").replace("北5", "北五")
        return nickname(value).contains(nickname(query), ignoreCase = true) ||
            nickname(format(value)).contains(nickname(query), ignoreCase = true)
    }

    fun formatSchedule(value: String) = value.lines().mapIndexed { index, line -> if (index == 0) line else format(line) }.joinToString("\n")
}

/** 固定全局键，所有账号和学期共用；不改写教务数据或单门课程的本地覆盖。 */
@Single
class ClassroomReplacementRepository(private val preferences: AppPreferenceRepository) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "classroom_replacement_rules"
    private fun decode(value: String?): List<ClassroomReplacementRule> = value?.let {
        runCatching { json.decodeFromString<List<ClassroomReplacementRule>>(it) }.getOrDefault(emptyList())
    } ?: emptyList()
    private suspend fun load(): List<ClassroomReplacementRule> {
        val saved = preferences.get(key)
        val entries = decode(saved)
        // Previously deleted defaults remain available, switched off rather than silently re-enabled.
        val rules = builtinNameRules.map { builtin ->
            entries.firstOrNull { it.id == builtin.id } ?: builtin.copy(enabled = saved == null || entries.isNotEmpty() && builtin.target == NameReplacementTarget.Course)
        } + entries.filterNot { it.isBuiltin }
        if (saved == null || entries != rules) preferences.set(key, json.encodeToString(rules))
        return rules
    }
    val rules = preferences.observe(key).onStart { mutex.withLock { load() } }.map(::decode)
    suspend fun get() = mutex.withLock { load() }

    suspend fun save(id: String?, source: String, replacement: String, target: NameReplacementTarget = NameReplacementTarget.Classroom) = mutex.withLock {
        require(id == null || builtinNameRules.none { it.id == id }) { "内置规则仅支持开启或关闭。" }
        val from = source.trim()
        val to = replacement.trim()
        require(from.isNotEmpty() && to.isNotEmpty()) { "请填写原文字和替换后的文字。" }
        require(from.length <= 100 && to.length <= 100) { "每项文字最多 100 个字符。" }
        require(from != to) { "替换后的文字应与原文字不同。" }
        val entries = load().toMutableList()
        require(entries.none { !it.isBuiltin && it.id != id && it.target == target && it.source == from }) { "已有相同原文字的规则，请编辑原规则。" }
        val index = entries.indexOfFirst { it.id == id }
        require(id == null || index >= 0) { "该规则已删除，请返回后重试。" }
        require(index >= 0 || entries.size < 100) { "最多添加 100 条规则。" }
        if (index >= 0) {
            val previous = entries[index]
            val modified = previous.source != from || previous.replacement != to
            entries[index] = previous.copy(source = from, replacement = to, target = target,
                campus = previous.campus.takeUnless { modified }, sourceUrl = previous.sourceUrl.takeUnless { modified })
        }
        else entries += ClassroomReplacementRule(Uuid.random().toString(), from, to, target = target)
        preferences.set(key, json.encodeToString(entries))
    }

    suspend fun enabled(id: String, enabled: Boolean) = enabled(setOf(id), enabled)

    suspend fun enabled(ids: Set<String>, enabled: Boolean) = mutex.withLock {
        preferences.set(key, json.encodeToString(load().map { if (it.id in ids) it.copy(enabled = enabled) else it }))
    }

    suspend fun delete(id: String) = delete(setOf(id))

    suspend fun delete(ids: Set<String>) = mutex.withLock {
        val remaining = load().filterNot { it.id in ids && !it.isBuiltin }
        preferences.set(key, json.encodeToString(remaining))
    }

}
