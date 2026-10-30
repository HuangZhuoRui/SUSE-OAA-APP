package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.person.EnrollmentGrade
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
        email: String,
        grade: Int
    ): Result<String> {
        if (!EnrollmentGrade.isValid(grade)) {
            return appFailure(AppError.Business(userMessage = EnrollmentGrade.ERROR_MESSAGE))
        }
        val request = RegisterRequest(
            studentId = studentId,
            name = name,
            username = username,
            password = password,
            email = email,
            grade = grade
        )
        return authApi.register(request).map { "注册成功" }
    }
}
