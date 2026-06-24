package com.chow.pomegranate.service.academic.affairs.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 选课课程操作结果。
 */
@Serializable
data class CourseOperateResult(
    @SerialName("success")
    val isSuccess: Boolean,
    @SerialName("message")
    val message: String? = null,
)