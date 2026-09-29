package com.chow.pomegranate.service.academic.notice.internal.parser

import com.chow.pomegranate.service.academic.notice.model.BasicNotice

/**
 * 通知列表页的解析结果。
 *
 * @property notices 本页通知
 * @property totalPages 服务端给出的总页数，页面未提供时为 `null`
 */
internal data class NoticeParseResult(
    val notices: List<BasicNotice>,
    val totalPages: Int? = null,
)
