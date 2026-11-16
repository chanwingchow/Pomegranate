package com.chow.pomegranate.network

import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.session.SecureStore

enum class ConnectionPolicy(val label: String) { Automatic("自动"), Direct("直连"), Proxy("代理") }
data class SavedWebVpnConnection(val policy: ConnectionPolicy, val account: String?, val cookies: String?)

interface WebVpnPersistence {
    suspend fun load(): SavedWebVpnConnection
    suspend fun policy(value: ConnectionPolicy)
    suspend fun save(account: String, cookies: String)
    suspend fun forget(account: String?)
}

internal class PreferenceWebVpnPersistence(private val preferences: AppPreferenceRepository, private val secrets: SecureStore) : WebVpnPersistence {
    private fun cookieKey(account: String) = "webvpn:cookies:$account"
    override suspend fun load(): SavedWebVpnConnection {
        val policy = preferences.get("connection_policy")?.let { runCatching { ConnectionPolicy.valueOf(it) }.getOrNull() } ?: ConnectionPolicy.Automatic
        val account = preferences.get("webvpn_account")
        return SavedWebVpnConnection(policy, account, account?.let { secrets.get(cookieKey(it)) })
    }
    override suspend fun policy(value: ConnectionPolicy) { preferences.set("connection_policy", value.name) }
    override suspend fun save(account: String, cookies: String) {
        secrets.put(cookieKey(account), cookies)
        preferences.set("webvpn_account", account)
    }
    override suspend fun forget(account: String?) {
        account?.let { secrets.remove(cookieKey(it)) }
        if (preferences.get("webvpn_account") == account) preferences.remove("webvpn_account")
    }
}
