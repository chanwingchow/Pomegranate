package com.chow.pomegranate.service.academic.notice.model

/**
 * 教务通知。
 *
 * @property title 标题
 * @property date 发布日期
 * @property urlString URL
 */
data class BasicNotice(
    val title: String,
    val date: String,
    val urlString: String,
)