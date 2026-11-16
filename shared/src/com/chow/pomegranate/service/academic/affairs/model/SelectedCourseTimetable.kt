package com.chow.pomegranate.service.academic.affairs.model

/** 本轮选课系统返回的课表，保留同一时段内的多门课及原始换行。 */
data class SelectedCourseTimetable(val rows: List<Row>) {
    data class Row(val sections: String, val days: List<String>)
}
