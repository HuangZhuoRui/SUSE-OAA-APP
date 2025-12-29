package com.suseoaa.projectoaa.core.network.model.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)