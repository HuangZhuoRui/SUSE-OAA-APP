package com.suseoaa.projectoaa.shared.domain.model.recruitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InterviewerEntry(
    @SerialName("user_id") val userId: Int,
    @SerialName("remark") val remark: String
)

/** POST /interviewer/create，一次为某个周期批量指定面试官。 */
@Serializable
data class CreateInterviewersRequest(
    @SerialName("term_id") val termId: Int,
    @SerialName("interviewers") val interviewers: List<InterviewerEntry>
)

@Serializable
data class UpdateInterviewerRequest(
    @SerialName("interviewer_id") val interviewerId: Int,
    @SerialName("remark") val remark: String
)

@Serializable
data class InterviewerIdRequest(
    @SerialName("interviewer_id") val interviewerId: Int
)

/**
 * 面试结果。
 *
 * GET /interviewer/result/list 文档没给响应结构，字段按创建 / 更新接口的请求体推测，
 * 全部可选。
 */
@Serializable
data class InterviewResult(
    @SerialName("interview_result_id") val interviewResultId: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("application_id") val applicationId: Int = 0,
    @SerialName("decision") val decision: String = "",
    @SerialName("result_department_id") val resultDepartmentId: Int = 0,
    @SerialName("result_role_id") val resultRoleId: Int = 0,
    @SerialName("remark") val remark: String = "",
    @SerialName("interviewer_id") val interviewerId: Int = 0,
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val resolvedId: Int get() = if (interviewResultId != 0) interviewResultId else id
}

/** 录入面试结论时界面提交的内容。 */
data class InterviewDecision(
    val decision: String,
    val resultDepartmentId: Int,
    val resultRoleId: Int,
    val remark: String
)

@Serializable
data class CreateInterviewResultRequest(
    @SerialName("application_id") val applicationId: Int,
    @SerialName("decision") val decision: String,
    @SerialName("result_department_id") val resultDepartmentId: Int,
    @SerialName("result_role_id") val resultRoleId: Int,
    @SerialName("remark") val remark: String
)

@Serializable
data class UpdateInterviewResultRequest(
    @SerialName("interview_result_id") val interviewResultId: Int,
    @SerialName("decision") val decision: String,
    @SerialName("result_department_id") val resultDepartmentId: Int,
    @SerialName("result_role_id") val resultRoleId: Int,
    @SerialName("remark") val remark: String
)
