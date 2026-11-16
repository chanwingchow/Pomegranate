package com.chow.pomegranate.ui.screen.main.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Assignment
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.automirrored.twotone.TrendingUp
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.ContactPage
import androidx.compose.material.icons.twotone.Groups
import androidx.compose.material.icons.twotone.NotificationsActive
import androidx.compose.material.icons.twotone.Phone
import androidx.compose.material.icons.twotone.RocketLaunch
import androidx.compose.material.icons.twotone.School
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Verified
import androidx.compose.material.icons.twotone.Web
import androidx.compose.ui.graphics.vector.ImageVector
import com.chow.pomegranate.Res
import com.chow.pomegranate.academic_plan
import com.chow.pomegranate.academic_program
import com.chow.pomegranate.academic_progress
import com.chow.pomegranate.academic_notice
import com.chow.pomegranate.cet
import com.chow.pomegranate.course_selection
import com.chow.pomegranate.course_timetable
import com.chow.pomegranate.emergency_contact
import com.chow.pomegranate.exam_schedule
import com.chow.pomegranate.extracurricular
import com.chow.pomegranate.otp
import com.chow.pomegranate.teachers
import com.chow.pomegranate.transcript
import com.chow.pomegranate.useful_websites
import org.jetbrains.compose.resources.StringResource

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
    ),

    /** 考试安排 */
    ExamSchedule(
        label = Res.string.exam_schedule,
        icon = Icons.TwoTone.ContactPage,
    ),

    /** CET */
    CET(
        label = Res.string.cet,
        icon = Icons.TwoTone.Verified,
    ),

    /** 课程成绩 */
    Transcript(
        label = Res.string.transcript,
        icon = Icons.AutoMirrored.TwoTone.Assignment,
    ),

    /** 选课系统 */
    CourseSelection(
        label = Res.string.course_selection,
        icon = Icons.TwoTone.School,
    ),

    /** 第二课堂 */
    Extracurricular(
        label = Res.string.extracurricular,
        icon = Icons.TwoTone.Groups,
    ),

    /** 教师信息 */
    Teachers(
        label = Res.string.teachers,
        icon = Icons.TwoTone.ContactPage,
    ),

    /** 全校课表 */
    CourseTimetable(
        label = Res.string.course_timetable,
        icon = Icons.TwoTone.CalendarMonth,
    ),

    /** 执行计划 */
    AcademicPlan(
        label = Res.string.academic_plan,
        icon = Icons.TwoTone.RocketLaunch,
    ),

    /** 学业进度 */
    AcademicProgress(
        label = Res.string.academic_progress,
        icon = Icons.AutoMirrored.TwoTone.TrendingUp,
    ),

    /** 培养方案 */
    AcademicProgram(
        label = Res.string.academic_program,
        icon = Icons.AutoMirrored.TwoTone.MenuBook,
    ),
}
