package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.notice.api.AcademicNotice
import com.chow.pomegranate.service.academic.notice.model.BasicNotice
import com.chow.pomegranate.service.academic.notice.model.Notice
import com.chow.pomegranate.service.academic.notice.model.NoticePage
import org.koin.core.annotation.Single

/** 教学通知数据仓库。 */
@Single
open class NoticeRepository(
    private val academicNotice: AcademicNotice,
) {
    /**
     * 返回第 [pageIndex] 页的通知列表及其分页信息。
     */
    open suspend fun getNotices(pageIndex: Int = 1): NoticePage {
        return academicNotice.getNotices(pageIndex)
    }

    /**
     * 返回 [notice] 的详情。
     *
     * 只做数据搬运，HTML 由界面层的渲染组件处理。
     */
    suspend fun getNotice(notice: BasicNotice): NoticeDetail {
        return when (val detail = academicNotice.getNotice(notice)) {
            is Notice.Html -> NoticeDetail(
                title = detail.title,
                date = detail.date,
                urlString = detail.urlString,
                html = detail.html,
            )

            is Notice.Document -> NoticeDetail(
                title = detail.title,
                date = detail.date,
                urlString = detail.urlString,
                pdfUrlString = detail.pdfUrlString,
            )
        }
    }
}

/**
 * 通知详情。
 *
 * @property title 标题
 * @property date 发布日期
 * @property urlString 原始链接
 * @property html HTML 正文，PDF 通知为空
 * @property pdfUrlString PDF 地址，仅 PDF 通知有值
 */
data class NoticeDetail(
    val title: String,
    val date: String,
    val urlString: String,
    val html: String = "",
    val pdfUrlString: String? = null,
)
