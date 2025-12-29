package com.suseoaa.projectoaa.core.network.model.register

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    @SerialName("code")
    val code: String,
    @SerialName("data")
    val `data`: Data,
    @SerialName("message")
    val message: String
) {
    @Serializable
    class Data
}