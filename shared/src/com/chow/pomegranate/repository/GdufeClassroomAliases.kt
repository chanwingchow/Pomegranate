package com.chow.pomegranate.repository

/** 官方页面确认的正式楼名 → 学生常用简称；不用含义不统一的“三教”等旧系统编号推断楼栋。 */
internal val gdufeClassroomAliases = listOf(
    ClassroomReplacementRule("builtin-gdufe-lexue", "乐学楼", "北五", campus = "广州校区",
        sourceUrl = "https://kjxy.gdufe.edu.cn/2024/0529/c4610a192850/page.htm"),
    ClassroomReplacementRule("builtin-gdufe-shangxue", "尚学楼", "北三", campus = "广州校区",
        sourceUrl = "https://mba.gdufe.edu.cn/10486/list.htm"),
    ClassroomReplacementRule("builtin-gdufe-lide", "立德楼", "北二", campus = "广州校区",
        sourceUrl = "https://gsjm.gdufe.edu.cn/2026/1001/c3596a243569/page.htm"),
    ClassroomReplacementRule("builtin-gdufe-boxue", "博学楼", "一教", campus = "广州校区",
        sourceUrl = "https://finance.gdufe.edu.cn/2025/0407/c4344a218042/page.htm"),
    ClassroomReplacementRule("builtin-gdufe-lixue", "励学楼", "一教", campus = "佛山校区",
        sourceUrl = "https://rlzy.gdufe.edu.cn/2025/1018/c7285a228962/page.htm"),
    ClassroomReplacementRule("builtin-gdufe-duxing", "笃行楼", "二教", campus = "佛山校区",
        sourceUrl = "https://rlzy.gdufe.edu.cn/2025/1018/c7285a228962/page.htm"),
    ClassroomReplacementRule("builtin-gdufe-tuoxin", "拓新楼", "实验楼", campus = "佛山校区",
        sourceUrl = "https://rlzy.gdufe.edu.cn/2025/1018/c7285a228962/page.htm"),
)

internal val builtinNameRules = gdufeClassroomAliases + commonCourseAliases
private val builtinNameRuleIds = builtinNameRules.map { it.id }.toSet()
internal val ClassroomReplacementRule.isBuiltin: Boolean get() = id in builtinNameRuleIds
