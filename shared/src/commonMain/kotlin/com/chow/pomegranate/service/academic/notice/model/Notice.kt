package com.chow.pomegranate.service.academic.notice.model

/**
 * 通知。
 *
 * @property title 标题
 * @property date 发布日期
 * @property urlString URL
 */
sealed interface Notice {
    val title: String
    val date: String
    val urlString: String

    /**
     * HTML 通知。
     *
     * @property html HTML
     */
    data class Html(
        override val title: String,
        override val date: String,
        override val urlString: String,
        val html: String,
    ) : Notice

    /**
     * PDF 通知
     *
     * @property pdfUrlString PDF URL
     */
    data class Document(
        override val title: String,
        override val date: String,
        override val urlString: String,
        val pdfUrlString: String,
    ) : Notice
}