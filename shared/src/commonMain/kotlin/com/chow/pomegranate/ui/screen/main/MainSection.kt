package com.chow.pomegranate.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Face4
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material.icons.twotone.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.me
import com.chow.pomegranate.modules
import com.chow.pomegranate.timetable

/**
 * 主页部分。
 *
 * @param label 标签
 * @param icon 图标
 */
enum class MainSection(
    val label: StringResource,
    val icon: ImageVector,
) {
    /**
     * 模块。
     */
    Modules(
        label = Res.string.modules,
        icon = Icons.TwoTone.ViewModule,
    ),

    /**
     * 课表。
     */
    Timetable(
        label = Res.string.timetable,
        icon = Icons.TwoTone.Schedule,
    ),

    /**
     * 我。
     */
    Me(
        label = Res.string.me,
        icon = Icons.TwoTone.Face4,
    )
}