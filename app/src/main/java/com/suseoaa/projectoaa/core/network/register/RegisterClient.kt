package com.suseoaa.projectoaa.core.network.register

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.suseoaa.projectoaa.core.network.BASE_URL_FOR_SUSE_OAA
import com.suseoaa.projectoaa.core.network.login.LoginService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object RegisterClient {
    //    配置Json解析器
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
    val apiService: RegisterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_FOR_SUSE_OAA)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RegisterService::class.java)
    }
}