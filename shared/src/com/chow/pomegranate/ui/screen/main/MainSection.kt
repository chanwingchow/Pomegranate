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
import com.chow.pomegranate.ui.screen.main.me.Gender

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

/**
 * 返回分区图标。
 *
 * "我"的图标跟随所选性别变化，其余分区用固定图标。
 *
 * @param gender 当前性别
 */
fun MainSection.icon(gender: Gender): ImageVector = when (this) {
    MainSection.Me -> gender.icon
    else -> icon
}
