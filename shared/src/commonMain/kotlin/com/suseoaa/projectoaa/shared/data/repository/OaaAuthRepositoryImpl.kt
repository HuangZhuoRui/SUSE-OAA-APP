package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.remote.oaaDevice
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.changePassword.ResetPasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SCENE_RESET_PASSWORD
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SendCodeRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.UpdatePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.repository.OaaAuthRepository

/**
 * OAA 后端登录与密码仓库
 */
class OaaAuthRepositoryImpl(
    private val authApi: OaaAuthApi,
    private val userApi: OaaUserApi,
    private val sessionStore: SessionStore,
    private val clearSession: suspend () -> Unit
) : OaaAuthRepository {

    override suspend fun login(account: String, password: String): Result<PersonData> {
        val tokens = authApi.login(LoginRequest(account = account, password = password, device = oaaDevice))
            .getOrElse { return Result.failure(it) }
        if (tokens.token.isEmpty()) {
            return appFailure(AppError.Business(userMessage = "登录失败：服务器未返回令牌"))
        }
        sessionStore.saveTokens(tokens.token, tokens.refreshToken)

        // 刷新 token 需要 user_id；教务等功能需要真正的学号（登录账号可能是用户名或邮箱）
        return userApi.me()
            .onSuccess { person ->
                sessionStore.saveUserId(person.userId)
                sessionStore.saveCurrentStudentId(person.studentId)
            }
            .onFailure {
                // 拿不到用户信息的会话无法续期，不如直接作废让用户重试
                clearSession()
            }
    }

    override suspend fun sendResetPasswordCode(account: String): Result<String> =
        authApi.sendCode(SendCodeRequest(account = account, scene = SCENE_RESET_PASSWORD))
            .map { "验证码已发送，请查收邮箱" }

    override suspend fun resetPassword(account: String, code: String, newPassword: String): Result<String> =
        authApi.resetPassword(
            ResetPasswordRequest(
                account = account,
                code = code,
                password = newPassword,
                scene = SCENE_RESET_PASSWORD
            )
        ).map { "密码已重置，请重新登录" }

    override suspend fun updatePassword(oldPassword: String, newPassword: String): Result<String> =
        authApi.updatePassword(UpdatePasswordRequest(oldPassword = oldPassword, newPassword = newPassword))
            .map { "密码修改成功，请重新登录" }
}
