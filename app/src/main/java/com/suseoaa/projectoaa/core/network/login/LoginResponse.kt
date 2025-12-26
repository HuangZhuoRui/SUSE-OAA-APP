package com.suseoaa.projectoaa.core.network.login


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: Data? = null,
    @SerialName("message")
    val message: String
) {
    @Serializable
    data class Data(
        @SerialName("token")
        val token: String
    )
}