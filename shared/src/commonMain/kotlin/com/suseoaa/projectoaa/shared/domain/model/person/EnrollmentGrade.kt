package com.suseoaa.projectoaa.shared.domain.model.person

/** 入学年份的统一约束；毕业状态始终使用后端结果，不由客户端按年份推算。 */
object EnrollmentGrade {
    const val ERROR_MESSAGE = "入学年份须为大于 2000 的四位数字（如 2024）"

    fun isValid(grade: Int): Boolean = grade in 2001..9999

    fun parse(text: String): Int? {
        val value = text.trim()
        if (value.length != 4 || value.any { it !in '0'..'9' }) return null
        return value.toIntOrNull()?.takeIf(::isValid)
    }
}
