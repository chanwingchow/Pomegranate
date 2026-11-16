package com.chow.pomegranate.service.cet.api

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

class CetTest {
    private lateinit var cet: CET

    @BeforeTest
    fun setUp() {
        cet = CET()
    }

    /**
     * 获取 CET 考试测试。
     */
    @Test
    fun getExamSchedule() = runBlocking {
        val cetExam = cet.getExamSchedule()

        println(cetExam)
    }
}