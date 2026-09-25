package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /user/list 的单条记录。 */
@Serializable
data class UserListItem(
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("role") val role: String = "",
    @SerialName("avatar") val avatar: StoredFile = StoredFile(),
    @SerialName("role_level") val roleLevel: Int? = null
)

@Serializable
data class UserPage(
    @SerialName("total") val total: Int = 0,
    @SerialName("list") val list: List<UserListItem> = emptyList()
)

/** 成员列表查询条件；空串表示不按该项筛选。 */
data class UserQuery(
    val keyword: String = "",
    val department: String = "",
    val role: String = "",
    val page: Int = 1,
    val pageSize: Int = 20
)

/** POST /user/batch 的单条变更。 */
@Serializable
data class UserBatchItem(
    @SerialName("user_id") val userId: Int,
    @SerialName("department_id") val departmentId: Int,
    @SerialName("role_id") val roleId: Int
)

/** /user/batch 返回的失败条目，全部成功时为空数组。 */
@Serializable
data class UserBatchFailure(
    @SerialName("id") val id: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("error_message") val errorMessage: String = ""
)

@Serializable
data class DeleteUserRequest(
    @SerialName("user_id") val userId: Int
)
