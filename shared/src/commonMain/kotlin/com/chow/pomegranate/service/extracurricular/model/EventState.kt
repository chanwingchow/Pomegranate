package com.chow.pomegranate.service.extracurricular.model

/**
 * 事件状态。
 */
enum class EventState(val id: Int) {
    /** 已报名 */
    Enrolled(0),

    /** 待开始 */
    PendingStart(1),

    /** 进行中 */
    Ongoing(2),

    /** 已完结 */
    Finished(3);
}