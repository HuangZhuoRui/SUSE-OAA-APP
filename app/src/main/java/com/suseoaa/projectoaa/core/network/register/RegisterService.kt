package com.suseoaa.projectoaa.core.network.register

import com.suseoaa.projectoaa.core.network.model.register.RegisterRequest
import com.suseoaa.projectoaa.core.network.model.register.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface RegisterService {
    @POST("/user/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}