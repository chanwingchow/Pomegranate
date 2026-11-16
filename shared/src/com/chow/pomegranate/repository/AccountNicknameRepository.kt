package com.chow.pomegranate.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

internal fun accountNicknameKey(userId: String, isDemo: Boolean) = Json.encodeToString(listOf(userId, isDemo.toString()))

/** 昵称只属于本机账号资料，不改变学号或学校登录信息。 */
@Single
class AccountNicknameRepository(private val preferences: AppPreferenceRepository) {
    private val key = "account_nicknames"
    private val mutex = Mutex()
    private fun decode(value: String?): Map<String, String> = value?.let {
        runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrDefault(emptyMap())
    } ?: emptyMap()
    val names = preferences.observe(key).map(::decode)
    suspend fun save(userId: String, isDemo: Boolean, nickname: String) = mutex.withLock {
        require(userId.isNotBlank()) { "请先选择账号。" }
        val name = nickname.trim()
        require(name.length <= 24 && name.none { it == '\n' || it == '\r' || it == '\t' }) { "昵称最多 24 个字符，请勿换行。" }
        val entries = decode(preferences.get(key)).toMutableMap()
        val account = accountNicknameKey(userId, isDemo)
        if (name.isBlank()) entries.remove(account) else entries[account] = name
        preferences.set(key, Json.encodeToString(entries))
    }
}
