package com.chow.pomegranate.service.academic.affairs.internal

import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import io.ktor.client.HttpClient

/**
 * 教务系统实现。
 */
internal class AcademicAffairsImpl(
    httpClient: HttpClient,
): AcademicAffairs {
    override val auth: AcademicAffairs.Auth = AuthImpl(httpClient)
}