package com.chow.pomegranate.service.academic.affairs.model

/**
 * 毕业审核。
 *
 * @property userId 学号
 * @property items 毕业审核项
 */
data class GraduationAudit(
    val userId: String,
    val items: List<GraduationAuditItem>,
)

/**
 * 毕业审核项。
 *
 * @property year 年份
 * @property batchName 批次名称
 * @property category 审核类别
 * @property enrollmentMethod 报名方式
 * @property degreeGpa 学位成绩绩点
 * @property completionRate 结业学分比率
 * @property enrollmentRate 报名学分比率
 * @property note 备注
 * @property reportUrlString 毕业审核报告文档 URL
 */
data class GraduationAuditItem(
    val year: String,
    val batchName: String,
    val category: String,
    val enrollmentMethod: String,
    val degreeGpa: Double,
    val completionRate: String,
    val enrollmentRate: String,
    val note: String?,
    val reportUrlString: String,
)