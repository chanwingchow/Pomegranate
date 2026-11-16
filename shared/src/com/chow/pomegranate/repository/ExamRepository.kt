package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.foundation.Semester
import org.koin.core.annotation.Single

/** 考试安排数据仓库。 */
@Single
class ExamRepository(
    private val academicAffairs: AcademicAffairs,
) {
    /**
     * 返回 [semester] 的考试安排。
     *
     * @param semester 学期，默认为当前学期
     */
    suspend fun getExamSchedule(semester: Semester = Semester.now()): ExamSchedule {
        return academicAffairs.enrollment.getExamSchedule(semester)
    }
}
