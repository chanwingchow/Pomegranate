package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.AcademicFeature
import com.chow.pomegranate.repository.AcademicFeatureItem
import com.chow.pomegranate.repository.FeatureDataModel

/** Features whose API response can contain enough records to merit local lookup. */
internal val AcademicFeature.supportsLocalSearch: Boolean
    get() = this in setOf(
        AcademicFeature.CourseTimetable,
        AcademicFeature.AcademicPlan,
    )

/**
 * 是否需要展示搜索框。
 *
 * 分页功能的关键词直接上送服务端，一次全量返回的功能在本地过滤。
 */
internal val AcademicFeature.isSearchable: Boolean
    get() = supportsLocalSearch || dataModel == FeatureDataModel.Paged

/** Searches all fields already returned by the API, without issuing another request. */
internal fun List<AcademicFeatureItem>.filterByKeyword(keyword: String): List<AcademicFeatureItem> {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return this
    return filter { item ->
        sequenceOf(item.title, item.supportingText)
            .plus(item.details.asSequence().flatMap { sequenceOf(it.first, it.second) })
            .any { it.contains(normalized, ignoreCase = true) }
    }
}
