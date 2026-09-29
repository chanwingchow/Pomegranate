package com.chow.pomegranate.repository

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.CourseTranscript
import com.chow.pomegranate.service.academic.affairs.model.LevelTranscript
import org.koin.core.annotation.Single

/** 成绩数据仓库。 */
@Single
class TranscriptRepository(
    private val academicAffairs: AcademicAffairs,
) {
    /** 返回课程成绩和等级成绩。 */
    suspend fun getTranscripts(): Transcripts {
        return Transcripts(
            course = academicAffairs.enrollment.getCourseTranscript(),
            level = academicAffairs.enrollment.getLevelTranscript(),
        )
    }
}

/** 成绩页所需的完整数据。 */
data class Transcripts(
    val course: CourseTranscript,
    val level: LevelTranscript,
)
