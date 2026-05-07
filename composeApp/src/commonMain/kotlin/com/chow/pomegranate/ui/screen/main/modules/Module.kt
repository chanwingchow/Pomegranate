package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.Phone
import androidx.compose.material.icons.twotone.Security
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.academic_notice
import pomegranate.composeapp.generated.resources.emergency_contact
import pomegranate.composeapp.generated.resources.otp

/**
 * 模块。
 *
 * @param label 标签
 * @param icon 图标
 */
enum class Module(
    val label: StringResource,
    val icon: ImageVector,
) {
    OTP(
        label = Res.string.otp,
        icon = Icons.TwoTone.Security,
    ),

    EmergencyContact(
        label = Res.string.emergency_contact,
        icon = Icons.TwoTone.Phone,
    ),

    AcademicNotice(
        label = Res.string.academic_notice,
        icon = Icons.TwoTone.NotificationsActive,
    )
}