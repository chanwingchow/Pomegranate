package com.chow.pomegranate.service.academic.notice.api

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcademicNoticeTest {
    private lateinit var academicNotice: AcademicNotice

    @BeforeTest
    fun setUp() {
        academicNotice = AcademicNotice()
    }

    /**
     * 获取教务通知列表测试。
     */
    @Test
    fun getNotices() = runBlocking {
        val page = academicNotice.getNotices()

        println(page)
    }

    /**
     * 分页信息测试。
     *
     * 列表页会给出“页码 1/78”这类信息，用于判断真正的末页，
     * 否则翻过末页后仍会返回 200，列表会一直显示正在加载。
     */
    @Test
    fun getNoticesParsesPagination() = runBlocking {
        val page = academicNotice.getNotices()

        assertEquals(1, page.pageIndex)
        assertTrue(page.notices.isNotEmpty())
        assertTrue((page.totalPages ?: 0) > 1, "应解析出总页数，实际为 ${page.totalPages}")
        assertTrue(page.hasMorePages)
    }

    /**
     * 获取教务通知详情测试。
     */
    @Test
    fun getNotice() = runBlocking {
        val page = academicNotice.getNotices(6)

        val notice = academicNotice.getNotice(page.notices.first())

        println(notice)
    }
}
