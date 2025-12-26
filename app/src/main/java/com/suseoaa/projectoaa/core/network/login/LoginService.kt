package com.suseoaa.projectoaa.core.network.login

import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}