package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.AcademicAlert
import com.chow.pomegranate.service.academic.affairs.model.AcademicPlan
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgram
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgress
import com.chow.pomegranate.service.academic.affairs.model.GraduationAudit
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 培养方案仓库测试。 */
class AcademicProgramRepositoryTest {
    /** 返回教务系统给出的 HTML。 */
    @Test
    fun returnProgramHtml() = runBlocking<Unit> {
        val html = "<form><table><tr><td>课程</td></tr></table></form>"
        val repository = AcademicProgramRepository(FakeAcademic(programHtml = html))

        val program = repository.getAcademicProgram()

        assertEquals("202400000001", program.userId)
        assertEquals(html, program.html)
    }

    /** 教务系统报错时向上抛出。 */
    @Test
    fun propagateFailure() = runBlocking<Unit> {
        val repository = AcademicProgramRepository(
            FakeAcademic(throwable = IllegalStateException("未登录")),
        )

        assertFailsWith<IllegalStateException> {
            repository.getAcademicProgram()
        }
    }
}

/**
 * 假的学业模块。
 *
 * @param programHtml 培养方案 HTML
 * @param throwable 非空时所有调用都会抛出该异常
 */
private class FakeAcademic(
    private val programHtml: String = "",
    private val throwable: Throwable? = null,
) : AcademicAffairs.Academic {
    override suspend fun getAcademicPlan(): AcademicPlan = fail()

    override suspend fun getAcademicProgress(isPrimary: Boolean): AcademicProgress = fail()

    override suspend fun getAcademicAlert(pageIndex: Int): AcademicAlert = fail()

    override suspend fun getAcademicProgram(): AcademicProgram {
        throwable?.let { throw it }

        return AcademicProgram(
            userId = "202400000001",
            html = programHtml,
        )
    }

    override suspend fun getGraduationAudit(isPrimary: Boolean): GraduationAudit = fail()

    override suspend fun getGraduationAuditReport(urlString: String): ByteArray = fail()

    /** 抛出异常，表示该测试不应调用此方法。 */
    private fun fail(): Nothing = error("本测试不应调用该方法。")
}
