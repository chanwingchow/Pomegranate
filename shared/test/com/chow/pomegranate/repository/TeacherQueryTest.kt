package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.BasicTeacher
import com.chow.pomegranate.service.academic.affairs.model.CampusCalendar
import com.chow.pomegranate.service.academic.affairs.model.CourseTimetable
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.academic.affairs.model.ExamSchedule
import com.chow.pomegranate.service.academic.affairs.model.Exemption
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import com.chow.pomegranate.service.academic.affairs.model.Teacher
import com.chow.pomegranate.service.academic.affairs.model.Teachers
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.service.extracurricular.api.ExtracurricularActivity
import com.chow.pomegranate.service.extracurricular.model.Transcript
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 教师查询测试。
 *
 * 钉住一个上线前的设计缺陷：教师列表原本在客户端把全部分页拉回来再本地过滤，
 * 一次进页面就是几十个串行请求，还得靠页数上限兜底（会静默丢数据）。
 */
class TeacherQueryTest {
    @Test
    fun teacherQueryOnlyFetchesTheRequestedPage() = runBlocking {
        val enrollment = FakeEnrollment()
        val repository = createRepository(enrollment)

        val page = repository.getItems(AcademicFeature.Teachers)

        assertEquals(listOf(1), enrollment.requestedPages)
        assertEquals(1, page.items.size)
        assertEquals("陈老师", page.items.single().title)
        assertFalse(page.hasMorePages, "接口说只有一页时不该继续翻")
    }

    @Test
    fun teacherQuerySendsTheKeywordToTheServer() = runBlocking {
        val enrollment = FakeEnrollment()
        val repository = createRepository(enrollment)

        repository.getItems(AcademicFeature.Teachers, query = "  陈  ")

        // 关键词必须上送服务端过滤，且去掉首尾空白
        assertEquals(listOf("陈"), enrollment.requestedQueries)
    }

    @Test
    fun teacherQueryReportsMorePagesWhenServerHasThem() = runBlocking {
        val repository = createRepository(FakeEnrollment(pageCount = 3))

        val page = repository.getItems(AcademicFeature.Teachers, pageIndex = 1)

        assertEquals(true, page.hasMorePages)
    }

    /** 一次返回全量的功能不该被分页影响：页码对它们没有意义，也不该多打请求。 */
    @Test
    fun wholeFeaturesDoNotTouchPagination() = runBlocking {
        val enrollment = FakeEnrollment()
        val repository = createRepository(enrollment)

        val page = repository.getItems(AcademicFeature.Extracurricular, pageIndex = 5)

        assertEquals(0, enrollment.requestedPages.size)
        assertFalse(page.hasMorePages)
    }

    private fun createRepository(enrollment: AcademicAffairs.Enrollment) =
        AcademicFeatureRepository(
            academicAffairs = FakeAcademicAffairs(enrollment),
            extracurricularActivity = FakeExtracurricular,
        )

    /** 只记录调用的教师列表假实现。 */
    private class FakeEnrollment(
        private val pageCount: Int = 1,
    ) : AcademicAffairs.Enrollment {
        val requestedPages = mutableListOf<Int>()
        val requestedQueries = mutableListOf<String>()

        override suspend fun getTeachers(
            query: String,
            pageIndex: Int,
        ): Teachers {
            requestedPages += pageIndex
            requestedQueries += query

            return Teachers(
                pageIndex = pageIndex,
                pageCount = pageCount,
                items = listOf(
                    BasicTeacher(id = "$pageIndex-1", name = "陈老师", department = "计算机学院"),
                ),
            )
        }

        override suspend fun getTimetable(semester: Semester): Timetable = error("not used")

        override suspend fun getCourseTimetable(semester: Semester): CourseTimetable = error("not used")

        override suspend fun getExamSchedule(semester: Semester): ExamSchedule = error("not used")

        override suspend fun getCourseTranscript(): CourseTranscript = error("not used")

        override suspend fun getLevelTranscript(): LevelTranscript = error("not used")

        override suspend fun getExemption(semester: Semester): Exemption = error("not used")

        override suspend fun getCampusCalendar(semester: Semester): CampusCalendar = error("not used")

        override suspend fun getTeacher(id: String): Teacher = error("not used")
    }

    /** 只提供 enrollment 的假实现；其余模块用 getter 占位，访问时才抛。 */
    private class FakeAcademicAffairs(
        override val enrollment: AcademicAffairs.Enrollment,
    ) : AcademicAffairs {
        override val auth: AcademicAffairs.Auth get() = error("not used")

        override val academic: AcademicAffairs.Academic get() = error("not used")

        override val courseSystem: AcademicAffairs.CourseSystem get() = error("not used")
    }
}

/**
 * 不会用到的第二课堂实现。
 *
 * 必须放在文件级惰性单例，不能写成测试类的属性：属性初始化器在构造测试实例时就会执行，
 * 这里用了 error() 占位会直接把每个用例炸在初始化阶段。
 */
private val FakeExtracurricular: ExtracurricularActivity by lazy {
    object : ExtracurricularActivity {
        override val auth: ExtracurricularActivity.Auth get() = error("not used")

        // 一次全量返回的功能需要能正常取到数据（这里给空成绩）
        override val achievement: ExtracurricularActivity.Achievement =
            object : ExtracurricularActivity.Achievement {
                override suspend fun getTranscript() = Transcript(userId = "test", items = emptyList())

                override suspend fun getTranscriptLogs() = error("not used")
            }

        override val activity: ExtracurricularActivity.Activity get() = error("not used")
    }
}
