package com.chow.pomegranate.ui.screen.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face4
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.me
import pomegranate.composeapp.generated.resources.modules
import pomegranate.composeapp.generated.resources.timetable

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
        icon = Icons.Rounded.ViewModule,
    ),

    /**
     * 课表。
     */
    Timetable(
        label = Res.string.timetable,
        icon = Icons.Rounded.Schedule,
    ),

    /**
     * 我。
     */
    Me(
        label = Res.string.me,
        icon = Icons.Rounded.Face4,
    )
}