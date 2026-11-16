package com.chow.pomegranate.service.extracurricular.model.response

/**
 * 第二课堂响应。
 *
 * @property code 状态码
 * @property msg 消息
 * @property data 数据
 */
interface ExtracurricularResponse<T> {
    val code: Int
    val msg: String
    val data: T?
}