package com.suseoaa.projectoaa.core.network.register


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("name")
    val name: String,
    @SerialName("password")
    val password: String,
    @SerialName("role")
    val role: String,
    @SerialName("student_id")
    val studentId: String,
    @SerialName("username")
    val username: String
)