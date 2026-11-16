package com.chow.pomegranate

import androidx.navigation3.runtime.NavKey
import com.chow.pomegranate.ui.component.StudyField
import com.chow.pomegranate.ui.component.StudySection
import com.chow.pomegranate.ui.screen.cet.CetNoteRoute
import com.chow.pomegranate.ui.screen.feature.CourseFiltersRoute
import com.chow.pomegranate.ui.screen.feature.TeacherSearchRoute
import com.chow.pomegranate.ui.screen.settings.WeekSettingRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableNotesRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableCourseRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableWeekRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableDateRoute
import com.chow.pomegranate.ui.screen.main.timetable.TimetableEditRoute
import com.chow.pomegranate.repository.TimetableLesson
import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.repository.CourseSearchFilters
import com.chow.pomegranate.service.academic.affairs.model.CourseSelectCategory
import com.chow.pomegranate.service.academic.affairs.model.Section
import com.chow.pomegranate.service.foundation.Campus
import com.chow.pomegranate.ui.screen.feature.CourseInstructionsRoute
import com.chow.pomegranate.ui.screen.login.SecondClassLoginRoute
import com.chow.pomegranate.ui.screen.login.LoginErrorRoute
import com.chow.pomegranate.ui.screen.main.me.ThemeColorRoute
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationStateTest {
    @Test fun timetableCourseSheetRestoresTeacherLinksAndAllCoursesInTheSlot() {
        val json = Json { serializersModule = savedStateConfiguration.serializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val route = TimetableCourseRoute(StudyDetailRoute("timetable:3:0:0", "同一时段的 2 门课程",
            sections = listOf(
                StudySection("数据结构", listOf(StudyField("教师", "林知远", teacherQuery = "林知远"))),
                StudySection("数据库原理", listOf(StudyField("教室", "实验楼 A302"))),
            )))
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }

    @Test fun instructionSheetsCanBeSavedAndRestoredWithTheAppRegistry() {
        val json = Json { serializersModule = savedStateConfiguration.serializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        listOf<NavKey>(TimetableWeekRoute(8), TimetableDateRoute(10L, 0L, 100L), TimetableEditRoute(TimetableLesson("user:term", "lesson", "数据结构", "林知远", "A301", "1-16周", 0, listOf(1, 2), "班级")), WeekSettingRoute(6), TimetableNotesRoute("备注\n详情"), TeacherSearchRoute("陈思雨"), CourseFiltersRoute(CourseSelectCategory.General, CourseSearchFilters("老师", 3, Section.Second, Campus.Foshan)), ThemeColorRoute(), ThemeColorRoute("80112233"), LoginErrorRoute("登录失败", "服务错误\n请稍后重试"), SecondClassLoginRoute, CetNoteRoute("第一条\n\n第二条"), CourseInstructionsRoute(StudySection("", listOf(StudyField("注意事项", "选课说明")))))
            .forEach { route -> assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route))) }
    }
}
