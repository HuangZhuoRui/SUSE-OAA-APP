package com.suseoaa.projectoaa.shared.domain.model.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 请求体一律不给默认值：全局 Json 没开 encodeDefaults，带默认值的字段等于默认值时不会被序列化。

/** POST /auth/login。[account] 可以是用户名、邮箱或学号。 */
@Serializable
data class LoginRequest(
    @SerialName("account") val account: String,
    @SerialName("password") val password: String,
    @SerialName("device") val device: String
)

/** 登录与刷新 token 返回的令牌对。 */
@Serializable
data class TokenPair(
    @SerialName("token") val token: String = "",
    @SerialName("refresh_token") val refreshToken: String = ""
)

/** POST /auth/refresh */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("device") val device: String
)

/** POST /auth/logout */
@Serializable
data class LogoutRequest(
    @SerialName("device") val device: String
)
