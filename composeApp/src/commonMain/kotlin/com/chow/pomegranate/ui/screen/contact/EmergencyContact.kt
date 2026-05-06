package com.chow.pomegranate.ui.screen.contact

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.LocalHospital
import androidx.compose.material.icons.twotone.LocalPolice
import androidx.compose.material.icons.twotone.NotificationImportant
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import pomegranate.composeapp.generated.resources.Res
import pomegranate.composeapp.generated.resources.campus_alarm
import pomegranate.composeapp.generated.resources.campus_police_station
import pomegranate.composeapp.generated.resources.guanzhou_police_station
import pomegranate.composeapp.generated.resources.outpatient_department

/**
 * 紧急联系电话。
 */
sealed interface EmergencyContact {
    /** 部门 */
    val department: StringResource

    /** 电话号码 */
    val phoneNumber: String

    /** 图标 */
    val icon: ImageVector

    /** 工作时间 */
    val businessHours: ClosedRange<LocalTime>?

    /**
     * 广州校区紧急联系电话。
     */
    enum class Canton(
        override val department: StringResource,
        override val phoneNumber: String,
        override val icon: ImageVector,
        override val businessHours: ClosedRange<LocalTime>? = null,
    ) : EmergencyContact {
        /** 门诊部急救 */
        OutpatientDepartment(
            department = Res.string.outpatient_department,
            phoneNumber = "13112234297",
            icon = Icons.TwoTone.LocalHospital,
        ),

        /** 校园报警 */
        CampusAlarm(
            department = Res.string.campus_alarm,
            phoneNumber = "020-84096060",
            icon = Icons.TwoTone.NotificationImportant,
        ),

        /** 校区警务室 */
        CampusPoliceStation(
            department = Res.string.campus_police_station,
            phoneNumber = "020-84096110",
            icon = Icons.TwoTone.LocalPolice,
            businessHours = LocalTime(8, 30)..LocalTime(17, 30),
        ),

        /** 官洲派出所 */
        GuangzhouPoliceOffice(
            department = Res.string.guanzhou_police_station,
            phoneNumber = "020-84092782",
            icon = Icons.TwoTone.LocalPolice,
        );
    }

    /**
     * 佛山校区紧急联系电话。
     */
    enum class Foshan(
        override val department: StringResource,
        override val phoneNumber: String,
        override val icon: ImageVector,
        override val businessHours: ClosedRange<LocalTime>? = null,
    ) : EmergencyContact {
        /** 门诊部急救 */
        OutpatientDepartment(
            department = Res.string.outpatient_department,
            phoneNumber = "18566890063",
            icon = Icons.TwoTone.LocalHospital,
        ),

        /** 校园报警 */
        CampusAlarm(
            department = Res.string.campus_alarm,
            phoneNumber = "0757-87828110",
            icon = Icons.TwoTone.NotificationImportant,
        ),
    }
}