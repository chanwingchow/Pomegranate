package com.chow.pomegranate.session

import io.ktor.client.HttpClient

/**
 * 教务系统的原始 Http 客户端。
 *
 * 与安装了会话校验的 [HttpClient] 区分开：登录、登出与会话校验本身都必须走原始客户端，
 * 否则会在登录失效时递归触发重新登录。
 *
 * @property client 底层客户端
 */
class SessionApiClient(
    val client: HttpClient,
)
