package com.suseoaa.projectoaa.shared.domain.model.changePassword

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 邮箱验证码的使用场景，目前后端只定义了重置密码。 */
const val SCENE_RESET_PASSWORD = "reset_password"

/**
 * POST /auth/send
 *
 * 文档的 schema 只列了 scene，但示例带 account；忘记密码时用户未登录，
 * 后端只能靠 account 找到要发验证码的邮箱，所以两个都传。
 */
@Serializable
data class SendCodeRequest(
    @SerialName("account") val account: String,
    @SerialName("scene") val scene: String
)

/**
 * POST /auth/password/reset
 *
 * 文档 schema 是 {code, scene}，示例是 {account, code, password}，两者取并集发送，
 * 后端按需取用。
 */
@Serializable
data class ResetPasswordRequest(
    @SerialName("account") val account: String,
    @SerialName("code") val code: String,
    @SerialName("password") val password: String,
    @SerialName("scene") val scene: String
)

/** POST /auth/password/update，已登录且记得旧密码时使用。 */
@Serializable
data class UpdatePasswordRequest(
    @SerialName("old_password") val oldPassword: String,
    @SerialName("new_password") val newPassword: String
)
