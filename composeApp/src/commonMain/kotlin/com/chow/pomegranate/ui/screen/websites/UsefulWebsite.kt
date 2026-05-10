package com.chow.pomegranate.ui.screen.websites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.twotone.AppSettingsAlt
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.Ballot
import androidx.compose.material.icons.twotone.NetworkWifi
import androidx.compose.material.icons.twotone.OndemandVideo
import androidx.compose.material.icons.twotone.Portrait
import androidx.compose.material.icons.twotone.School
import androidx.compose.material.icons.twotone.SpaceDashboard
import androidx.compose.material.icons.twotone.Stadium
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.academic_affairs_system
import pomegranate.composeapp.generated.resources.campus_network
import pomegranate.composeapp.generated.resources.convergence_portal
import pomegranate.composeapp.generated.resources.extracurricular_activities
import pomegranate.composeapp.generated.resources.financial_system
import pomegranate.composeapp.generated.resources.gdufe_blackboard
import pomegranate.composeapp.generated.resources.gdufe_mooc
import pomegranate.composeapp.generated.resources.integrated_platform
import pomegranate.composeapp.generated.resources.gdufe_name
import pomegranate.composeapp.generated.resources.library

/**
 * 常用网站。
 *
 * @property label 名称
 * @property icon 图标
 * @property urlString 链接
 */
enum class UsefulWebsite(
    val label: StringResource,
    val icon: ImageVector,
    val urlString: String,
) {
    /** 校园网 */
    CampusNetwork(
        label = Res.string.campus_network,
        icon = Icons.TwoTone.NetworkWifi,
        urlString = "http://100.64.13.17",
    ),

    /** 图书馆 */
    Library(
        label = Res.string.library,
        icon = Icons.Outlined.LocalLibrary,
        urlString = "https://lib.gdufe.edu.cn",
    ),

    /** 融合门户 */
    ConvergencePortal(
        label = Res.string.convergence_portal,
        icon  = Icons.TwoTone.Portrait,
        urlString = "https://imy.gdufe.edu.cn",
    ),

    /** 一体化平台 */
    IntegratedPlatform(
        label = Res.string.integrated_platform,
        icon = Icons.TwoTone.Ballot,
        urlString = "https://sec.gdufe.edu.cn",
    ),

    /** 教务系统 */
    AcademicAffairsSystem(
        label = Res.string.academic_affairs_system,
        icon = Icons.TwoTone.School,
        urlString = "http://jwxt.gdufe.edu.cn/jsxsd",
    ),

    /** 第二课堂 */
    ExtracurricularActivities(
        label = Res.string.extracurricular_activities,
        icon = Icons.TwoTone.Stadium,
        urlString = "http://2ketang.gdufe.edu.cn",
    ),

    /** 广财慕课 */
    GdufeMooc(
        label = Res.string.gdufe_mooc,
        icon = Icons.TwoTone.OndemandVideo,
        urlString = "https://www.gdufemooc.cn",
    ),

    /** 毕博平台 */
    GdufeBlackboard(
        label = Res.string.gdufe_blackboard,
        icon = Icons.TwoTone.SpaceDashboard,
        urlString = "https://bb.gdufe.edu.cn",
    ),

    /** 财务系统 */
    FinancialSystem(
        label = Res.string.financial_system,
        icon = Icons.TwoTone.AttachMoney,
        urlString = "https://cwsfxt.gdufe.edu.cn",
    ),

    /** 广财通 */
    GdufeApp(
        label = Res.string.gdufe_name,
        icon = Icons.TwoTone.AppSettingsAlt,
        urlString = "https://gctzy.gdufe.edu.cn",
    ),
}