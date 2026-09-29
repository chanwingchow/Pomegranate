package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.AcademicProgram
import org.koin.core.annotation.Single

/** 培养方案数据仓库。 */
@Single
class AcademicProgramRepository(
    private val academic: AcademicAffairs.Academic,
) {
    /** 返回培养方案。 */
    suspend fun getAcademicProgram(): AcademicProgram {
        return academic.getAcademicProgram()
    }
}
