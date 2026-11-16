package com.chow.pomegranate.repository

import kotlin.test.*

class NameReplacementPriorityTest {
    @Test fun courseOverridesWinOverCustomRulesAndThenBuiltinsWithoutChangingTheSource() {
        val raw = "毛泽东思想和中国特色社会主义理论体系概论（上）"
        val builtin = ClassroomReplacements(commonCourseAliases, NameReplacementTarget.Course)
        assertEquals("毛概（上）", builtin.format(raw))
        val custom = ClassroomReplacementRule("user", "毛泽东思想和中国特色社会主义理论体系概论", "我的思政课", target = NameReplacementTarget.Course)
        val rules = ClassroomReplacements(commonCourseAliases + custom, NameReplacementTarget.Course)
        assertEquals("我的思政课（上）", rules.format(raw))
        assertEquals("毛泽东思想复习课", rules.format(raw, "毛泽东思想复习课"))
        assertEquals("毛概（上）", ClassroomReplacements(commonCourseAliases + custom.copy(enabled = false), NameReplacementTarget.Course).format(raw))
        assertEquals("毛泽东思想和中国特色社会主义理论体系概论（上）", raw)
    }

    @Test fun classroomOverridesBypassAllGlobalRulesAndTheTwoTargetsStaySeparate() {
        val raw = "乐学楼503"
        val custom = ClassroomReplacementRule("user", "乐学楼", "我的教学楼")
        val rules = ClassroomReplacements(builtinNameRules + custom)
        assertEquals("我的教学楼503", rules.format(raw))
        assertEquals("乐学楼701", rules.format(raw, "乐学楼701"))
        assertEquals("高等数学A（下）", rules.format("高等数学A（下）"))
        assertEquals("高数A（下）", ClassroomReplacements(builtinNameRules, NameReplacementTarget.Course).format("高等数学A（下）"))
    }
}
