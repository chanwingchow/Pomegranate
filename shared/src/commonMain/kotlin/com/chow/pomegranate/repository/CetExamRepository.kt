package com.chow.pomegranate.repository

import com.chow.pomegranate.service.cet.api.CET
import com.chow.pomegranate.service.cet.model.CetExam
import org.koin.core.annotation.Singleton

/**
 * [CetExam] 存储库。
 *
 * @property cet CET
 */
@Singleton
class CetExamRepository(
    private val cet: CET,
) {
    /**
     * 返回 [CetExam]。
     */
    suspend fun getCetExam(): CetExam {
        val cetExam = cet.getExamSchedule()

        return cetExam
    }

    /**
     * 返回 CET 考试安排 PDF 内容。
     */
    suspend fun getCetExamPdf(urlString: String): ByteArray {
        return cet.getExamSchedulePdf(urlString)
    }
}