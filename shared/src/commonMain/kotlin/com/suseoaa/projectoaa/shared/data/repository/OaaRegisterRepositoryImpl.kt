package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.domain.model.register.RegisterRequest
import com.suseoaa.projectoaa.shared.domain.repository.OaaRegisterRepository

/**
 * OAA 后端注册仓库
 */
class OaaRegisterRepositoryImpl(
    private val authApi: OaaAuthApi
) : OaaRegisterRepository {
    override suspend fun register(
        studentId: String,
        name: String,
        username: String,
        password: String,
        email: String
    ): Result<String> {
        val request = RegisterRequest(
            studentId = studentId,
            name = name,
            username = username,
            password = password,
            email = email
        )
        return authApi.register(request).map { "注册成功" }
    }
}
