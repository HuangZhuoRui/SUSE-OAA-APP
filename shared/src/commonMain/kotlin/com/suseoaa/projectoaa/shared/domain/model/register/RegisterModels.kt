package com.suseoaa.projectoaa.shared.domain.model.register

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /auth/register。新用户默认加入「开放原子开源协会」，职位为「会员」。 */
@Serializable
data class RegisterRequest(
    @SerialName("name") val name: String,
    @SerialName("password") val password: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String
)
