package com.chow.pomegranate.repository

/** 高校课程说明和教师讲义使用的简称；课程编号、分级和学期后缀仍保留。 */
internal val commonCourseAliases = buildList {
    val politics = "https://art.pku.edu.cn/rcpyjxxm/bks/tzgg_bks/aeac577879724136b1dcd6b2c90bbb5f.htm"
    fun alias(id: String, source: String, short: String, url: String) {
        add(ClassroomReplacementRule("builtin-course-$id", source, short, sourceUrl = url, target = NameReplacementTarget.Course))
    }
    alias("maogai", "毛泽东思想和中国特色社会主义理论体系概论", "毛概", politics)
    alias("mayuan-old", "马克思主义基本原理概论", "马原", politics)
    alias("mayuan", "马克思主义基本原理", "马原", politics)
    alias("shigang", "中国近现代史纲要", "史纲", politics)
    alias("sixiu", "思想道德修养与法律基础", "思修", politics)
    alias("xingce", "形势与政策", "形策", politics)
    alias("defa", "思想道德与法治", "德法", "https://mks.cqust.edu.cn/info/1170/1820.htm")
    alias("xigai", "习近平新时代中国特色社会主义思想概论", "习概", "https://som.sthu.edu.cn/xg/list.htm")
    alias("gaoshu", "高等数学", "高数", "https://www.icourse163.org/course/SYJZ-1474207193")
    alias("xiandai", "线性代数", "线代", "https://science.sustech.edu.cn/portal/article/index/id/1156")
    alias("gaitong", "概率论与数理统计", "概统", "https://liyingli-math.github.io/teaching/mat212-2023f/chap1.pdf")
}
