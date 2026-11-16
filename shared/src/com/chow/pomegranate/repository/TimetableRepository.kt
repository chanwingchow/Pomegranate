package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.foundation.Semester
import org.koin.core.annotation.Single

/** 课表数据仓库。 */
@Single
class TimetableRepository(
    private val academicAffairs: AcademicAffairs,
) {
    /**
     * 返回 [semester] 的课表。
     *
     * @param semester 学期，默认为当前学期
     */
    suspend fun getTimetable(semester: Semester = Semester.now()): Timetable {
        return academicAffairs.enrollment.getTimetable(semester)
    }
}
