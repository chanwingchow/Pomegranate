package com.chow.pomegranate.service.academic.notice.api

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

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
        val notices = academicNotice.getNotices()

        println(notices)
    }

    /**
     * 获取教务通知详情测试。
     */
    @Test
    fun getNotice() = runBlocking {
        val notices = academicNotice.getNotices(6)

        val notice = academicNotice.getNotice(notices[5])

        println(notice)
    }
}