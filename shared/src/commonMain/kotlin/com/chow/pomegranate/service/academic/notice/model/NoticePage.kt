package com.chow.pomegranate.service.academic.notice.model

/**
 * 一页通知列表及其分页信息。
 *
 * @property pageIndex 本页页码，从 1 开始
 * @property notices 本页通知
 * @property totalPages 服务端给出的总页数，页面未提供时为 `null`
 * @property hasMorePages 服务端是否还有下一页；`totalPages` 为空时退化为“本页非空”
 */
data class NoticePage(
    val pageIndex: Int,
    val notices: List<BasicNotice>,
    val totalPages: Int? = null,
    val hasMorePages: Boolean = notices.isNotEmpty(),
)
