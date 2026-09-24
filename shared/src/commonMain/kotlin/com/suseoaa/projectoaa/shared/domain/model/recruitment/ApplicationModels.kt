package com.suseoaa.projectoaa.shared.domain.model.recruitment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 表单里的固定选项。 */
object ApplicationOptions {
    val genders = listOf("男", "女")
    val politicalStatuses = listOf("中共党员", "中共预备党员", "共青团员", "群众")
}

/** 一个志愿：部门 + 职位。 */
@Serializable
data class Choice(
    @SerialName("department_id") val departmentId: Int = 0,
    @SerialName("role_id") val roleId: Int = 0
) {
    val isComplete: Boolean get() = departmentId > 0 && roleId > 0
}

/**
 * 申请表。
 *
 * /application/me 与 /application/list 共用；后者文档没给结构，推测会额外带申请人信息，
 * 所以 [name]、[studentId]、[username] 都是可选的。
 */
@Serializable
data class Application(
    @SerialName("application_id") val applicationId: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("term_id") val termId: Int = 0,
    @SerialName("type") val type: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("college") val college: String = "",
    @SerialName("major_class") val majorClass: String = "",
    @SerialName("gender") val gender: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("qq") val qq: String = "",
    @SerialName("political_status") val politicalStatus: String = "",
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("first_choice") val firstChoice: Choice = Choice(),
    @SerialName("second_choice") val secondChoice: Choice = Choice(),
    @SerialName("allow_adjust") val allowAdjust: Boolean = false,
    @SerialName("resume") val resume: String = "",
    @SerialName("reason") val reason: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("decision") val decision: String = "",
    @SerialName("result") val result: Choice? = null,
    @SerialName("operator_id") val operatorId: Int = 0
) {
    /** 文档里 /application/me 没有返回 ID，删除却要 application_id；两个字段都兼容，拿不到时为 0。 */
    val resolvedId: Int get() = if (applicationId != 0) applicationId else id

    fun toForm() = ApplicationForm(
        college = college,
        majorClass = majorClass,
        gender = gender,
        phone = phone,
        qq = qq,
        politicalStatus = politicalStatus,
        birthDate = birthDate,
        firstChoice = firstChoice,
        secondChoice = secondChoice,
        allowAdjust = allowAdjust,
        resume = resume,
        reason = reason
    )
}

/** 申请表的可编辑部分，界面状态直接用它。 */
data class ApplicationForm(
    val college: String = "",
    val majorClass: String = "",
    val gender: String = "",
    val phone: String = "",
    val qq: String = "",
    val politicalStatus: String = "",
    val birthDate: String = "",
    val firstChoice: Choice = Choice(),
    val secondChoice: Choice = Choice(),
    val allowAdjust: Boolean = true,
    val resume: String = "",
    val reason: String = ""
)

@Serializable
data class ChoiceRequest(
    @SerialName("department_id") val departmentId: Int,
    @SerialName("role_id") val roleId: Int
)

fun Choice.toRequest() = ChoiceRequest(departmentId = departmentId, roleId = roleId)

@Serializable
data class CreateApplicationRequest(
    @SerialName("term_id") val termId: Int,
    @SerialName("college") val college: String,
    @SerialName("major_class") val majorClass: String,
    @SerialName("gender") val gender: String,
    @SerialName("phone") val phone: String,
    @SerialName("qq") val qq: String,
    @SerialName("political_status") val politicalStatus: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("first_choice") val firstChoice: ChoiceRequest,
    @SerialName("second_choice") val secondChoice: ChoiceRequest,
    @SerialName("allow_adjust") val allowAdjust: Boolean,
    @SerialName("resume") val resume: String,
    @SerialName("reason") val reason: String
)

/**
 * POST /application/update
 *
 * 文档里没有 application_id / term_id，后端大概率按「当前用户 + 当前周期」定位；
 * 能拿到时一并带上，拿不到时（null）不序列化。
 */
@Serializable
data class UpdateApplicationRequest(
    @SerialName("application_id") val applicationId: Int? = null,
    @SerialName("term_id") val termId: Int? = null,
    @SerialName("college") val college: String,
    @SerialName("major_class") val majorClass: String,
    @SerialName("gender") val gender: String,
    @SerialName("phone") val phone: String,
    @SerialName("qq") val qq: String,
    @SerialName("political_status") val politicalStatus: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("first_choice") val firstChoice: ChoiceRequest,
    @SerialName("second_choice") val secondChoice: ChoiceRequest,
    @SerialName("allow_adjust") val allowAdjust: Boolean,
    @SerialName("resume") val resume: String,
    @SerialName("reason") val reason: String
)

@Serializable
data class ApplicationIdRequest(
    @SerialName("application_id") val applicationId: Int
)
