package com.suseoaa.projectoaa.shared.domain.model.recruitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 周期类型。 */
object TermTypes {
    const val RECRUITMENT = "招新"
    const val ELECTION = "换届"
    val all = listOf(RECRUITMENT, ELECTION)
}

/** 时间窗，日期格式为 yyyy-MM-dd，两端都包含。 */
@Serializable
data class Period(
    @SerialName("start_at") val startAt: String = "",
    @SerialName("end_at") val endAt: String = ""
)

/**
 * 一轮招新 / 换届。
 *
 * [editPeriod] 内可以提交、修改申请；[queryPeriod] 内可以查看录取结果。
 */
@Serializable
data class Term(
    @SerialName("term_id") val termId: Int = 0,
    // 列表接口同时返回了 term_id 和 id，含义应当相同；以 term_id 为准，缺失时退回 id
    @SerialName("id") val id: Int = 0,
    @SerialName("year") val year: Int = 0,
    @SerialName("type") val type: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("edit_period") val editPeriod: Period = Period(),
    @SerialName("query_period") val queryPeriod: Period = Period(),
    @SerialName("is_executed") val isExecuted: Boolean = false,
    @SerialName("executed_at") val executedAt: String? = null
) {
    val resolvedId: Int get() = if (termId != 0) termId else id
}

/** 请求体里的时间窗，不带默认值以保证总被序列化。 */
@Serializable
data class PeriodRequest(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String
)

@Serializable
data class CreateTermRequest(
    @SerialName("year") val year: Int,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("edit_period") val editPeriod: PeriodRequest,
    @SerialName("query_period") val queryPeriod: PeriodRequest
)

@Serializable
data class UpdateTermRequest(
    @SerialName("term_id") val termId: Int,
    @SerialName("title") val title: String,
    @SerialName("edit_period") val editPeriod: PeriodRequest,
    @SerialName("query_period") val queryPeriod: PeriodRequest
)

@Serializable
data class TermIdRequest(
    @SerialName("term_id") val termId: Int
)

fun Period.toRequest() = PeriodRequest(startAt = startAt, endAt = endAt)
