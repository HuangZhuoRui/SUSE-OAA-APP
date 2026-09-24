package com.suseoaa.projectoaa.shared.domain.model.org

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 部门类型：普通部门，或协会本身这样的顶层节点。 */
object DepartmentTypes {
    const val DEPARTMENT = "部门"
    const val ASSOCIATION = "协会"
    val all = listOf(DEPARTMENT, ASSOCIATION)
}

/** GET /department/list */
@Serializable
data class Department(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String = "",
    // 列表接口的文档没有这个字段，但更新接口有；缺省按启用处理
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

/** GET /role/list */
@Serializable
data class Role(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("level") val level: Int = 0,
    @SerialName("type") val type: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateDepartmentRequest(
    @SerialName("name") val name: String,
    @SerialName("type") val type: String
)

@Serializable
data class UpdateDepartmentRequest(
    @SerialName("department_id") val departmentId: Int,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class CreateRoleRequest(
    @SerialName("name") val name: String,
    @SerialName("level") val level: Int,
    @SerialName("type") val type: String
)

@Serializable
data class UpdateRoleRequest(
    @SerialName("role_id") val roleId: Int,
    @SerialName("name") val name: String,
    @SerialName("level") val level: Int,
    @SerialName("type") val type: String,
    @SerialName("is_active") val isActive: Boolean
)
