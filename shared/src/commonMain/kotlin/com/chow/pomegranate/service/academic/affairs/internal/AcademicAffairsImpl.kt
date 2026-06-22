package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 教务系统实现。
 */
internal class AcademicAffairsImpl(
    httpClient: HttpClient,
) : AcademicAffairs {
    private val userId = MutableStateFlow<String?>(null)

    override val auth: AcademicAffairs.Auth = AuthImpl(
        httpClient,
        userId = userId,
    )

    override val enrollment: AcademicAffairs.Enrollment = EnrollmentImpl(
        httpClient,
        userId = userId,
    )
}