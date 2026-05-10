package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.Phone
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Web
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_notice
import com.chow.pomegranate.emergency_contact
import com.chow.pomegranate.otp
import com.chow.pomegranate.useful_websites

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
    /** OTP */
    OTP(
        label = Res.string.otp,
        icon = Icons.TwoTone.Security,
    ),

    /** 紧急电话 */
    EmergencyContact(
        label = Res.string.emergency_contact,
        icon = Icons.TwoTone.Phone,
    ),

    /** 教学通知 */
    AcademicNotice(
        label = Res.string.academic_notice,
        icon = Icons.TwoTone.NotificationsActive,
    ),

    /** 常用网站 */
    UsefulWebsites(
        label = Res.string.useful_websites,
        icon = Icons.TwoTone.Web,
    )
}