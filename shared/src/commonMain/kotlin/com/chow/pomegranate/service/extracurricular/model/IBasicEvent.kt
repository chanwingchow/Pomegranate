package com.chow.pomegranate.service.extracurricular.model

/**
 * 基础事件接口。
 *
 * @property id 唯一标识
 * @property name 活动名称
 * @property category 分类
 * @property score 分数
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property organizer 主办方
 * @property imageUrl 封面
 * @property eventType 类型名称
 * @property isOnline 是否线上
 */
interface IBasicEvent {
    val id: Int
    val name: String
    val category: String
    val score: Double
    val startTime: Long
    val endTime: Long
    val organizer: String
    val imageUrl: String?
    val eventType: String
    val isOnline: Boolean
}
