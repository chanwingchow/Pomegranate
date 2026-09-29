package com.chow.pomegranate

import kotlinx.io.SystemLineSeparator
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readString
import kotlinx.io.files.SystemFileSystem

/**
 * 本地集成测试用的账号凭据。
 *
 * 从仓库根目录的 `local.properties` 读取，例如：
 * ```
 * user.name=学号
 * user.password=教务系统密码
 * user.extracurricularPassword=第二课堂密码
 * ```
 *
 * 这些测试需要真实校园网与真实账号，CI 或无凭据的开发机上应当跳过，
 * 而不是以失败告终——否则真实的回归会被一片联网失败淹没。
 */
object LocalTestCredentials {
    /** 教务系统凭据。 */
    val academicAffairs: Credentials? by lazy {
        read()?.let { properties ->
            val userId = properties["user.name"]
            val password = properties["user.password"]

            if (userId.isNullOrBlank() || password.isNullOrBlank()) {
                null
            } else {
                Credentials(userId, password)
            }
        }
    }

    /** 第二课堂凭据。 */
    val extracurricular: Credentials? by lazy {
        read()?.let { properties ->
            val userId = properties["user.name"]
            val password = properties["user.extracurricularPassword"]

            if (userId.isNullOrBlank() || password.isNullOrBlank()) {
                null
            } else {
                Credentials(userId, password)
            }
        }
    }

    private fun read(): Map<String, String>? = runCatching {
        val lines = SystemFileSystem.source(Path("../local.properties"))
            .buffered()
            .readString()
            .split(SystemLineSeparator)

        val properties = mutableMapOf<String, String>()

        for (line in lines) {
            if (!line.startsWith("user") || !line.contains('=')) continue

            val separator = line.indexOf('=')
            val key: String = line.substring(0, separator).trim()
            val value: String = line.substring(separator + 1).trim()
            properties[key] = value
        }

        properties
    }.getOrNull()

    /** 一组账号凭据。 */
    data class Credentials(
        val userId: String,
        val password: String,
    )
}
