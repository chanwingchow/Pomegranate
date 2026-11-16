package com.chow.pomegranate.ui.screen.main.me

import com.chow.pomegranate.Res
import com.chow.pomegranate.campus_canton
import com.chow.pomegranate.campus_foshan
import com.chow.pomegranate.service.foundation.Campus
import org.jetbrains.compose.resources.StringResource

/**
 * 校区展示信息。
 *
 * @property campus 校区
 * @property label 校区名称
 */
enum class CampusOption(
    val campus: Campus,
    val label: StringResource,
) {
    /** 广州校区 */
    Canton(
        campus = Campus.Canton,
        label = Res.string.campus_canton,
    ),

    /** 佛山校区 */
    Foshan(
        campus = Campus.Foshan,
        label = Res.string.campus_foshan,
    ),
    ;

    /** 切换到另一个校区。 */
    fun toggle(): CampusOption = if (this == Canton) Foshan else Canton

    companion object {
        /** 默认校区。 */
        val Default = Canton

        /**
         * 由偏好下标返回校区。
         */
        fun fromOrdinal(ordinal: Int?): CampusOption =
            entries.getOrNull(ordinal ?: -1) ?: Default
    }
}
