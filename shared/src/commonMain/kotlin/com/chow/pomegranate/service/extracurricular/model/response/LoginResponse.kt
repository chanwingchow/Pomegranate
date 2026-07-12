package com.chow.pomegranate.service.extracurricular.model.response

import kotlinx.serialization.Serializable

/**
 * 登录响应。
 */
@Serializable
data class LoginResponse(
    override val code: Int,
    override val msg: String,
    override val data: LoginData?,
) : ExtracurricularResponse<LoginData>

/**
 * 登录数据。
 *
 * @property id 第二课堂 id
 */
@Serializable
data class LoginData(
    val id: String,
)