package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.model.SelectedCourse
import com.chow.pomegranate.service.extracurricular.model.ExtracurricularGrade
import kotlin.test.Test
import kotlin.test.assertEquals

class AcademicFeatureRepositoryTest {
    @Test
    fun selectedCourseKeepsRealCourseDetails() {
        val item = SelectedCourse(
            id = "operation-id",
            courseId = "CS101",
            name = "程序设计",
            credits = 3.0,
            attribute = "必修",
            teacher = "陈老师",
            timeString = "1-16周 星期一 1-2节",
            classroom = "实验楼 201",
        ).toFeatureItem(batchName = "第一轮选课")

        assertEquals("operation-id", item.id)
        assertEquals("陈老师 · 实验楼 201", item.supportingText)
        assertEquals("CS101", item.details.toMap()["课程编号"])
        assertEquals("第一轮选课", item.details.toMap()["选课批次"])
    }

    @Test
    fun extracurricularGradeIncludesOptionalScores() {
        val item = ExtracurricularGrade(
            categoryId = 7,
            name = "思想成长",
            score = 3.5,
            requiredScore = 2.0,
            totalScore = 8.0,
            activityScore = 2.5,
            projectScore = 0.5,
            awardScore = 0.5,
            averageScore = 3.0,
            termMinScore = 1.0,
        ).toFeatureItem()

        assertEquals("3.5 / 2.0", item.supportingText)
        assertEquals("0.5", item.details.toMap()["奖励分数"])
        assertEquals("1.0", item.details.toMap()["学期最低分"])
    }
}
