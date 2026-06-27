package com.chow.pomegranate.service.academic.affairs.model

/**
 * 培养方案。
 *
 * @property userId 学号
 * @property html 培养方案 HTML
 */
data class AcademicProgram(
    val userId: String,
    val html: String,
)