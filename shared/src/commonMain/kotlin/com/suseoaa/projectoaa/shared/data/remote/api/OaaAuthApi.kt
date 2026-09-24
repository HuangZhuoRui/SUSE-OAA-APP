package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.domain.model.changePassword.ResetPasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.SendCodeRequest
import com.suseoaa.projectoaa.shared.domain.model.changePassword.UpdatePasswordRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LoginRequest
import com.suseoaa.projectoaa.shared.domain.model.login.LogoutRequest
import com.suseoaa.projectoaa.shared.domain.model.login.TokenPair
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest

/**
 * `/auth` 下的接口：注册、登录、登出、密码。
 *
 * 刷新 token 不在这里——它必须走不带 401 拦截的裸客户端，见 OaaTokenManager。
 */
class OaaAuthApi(private val requester: OaaRequester) {

    suspend fun login(request: LoginRequest): Result<TokenPair> =
        requester.post("/auth/login", request, "登录失败")

    suspend fun register(request: RegisterRequest): Result<Unit> =
        requester.post("/auth/register", request, "注册失败")

    suspend fun logout(request: LogoutRequest): Result<Unit> =
        requester.post("/auth/logout", request, "退出登录失败")

    suspend fun sendCode(request: SendCodeRequest): Result<Unit> =
        requester.post("/auth/send", request, "验证码发送失败")

    suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit> =
        requester.post("/auth/password/reset", request, "重置密码失败")

    suspend fun updatePassword(request: UpdatePasswordRequest): Result<Unit> =
        requester.post("/auth/password/update", request, "修改密码失败")
}
