package com.suseoaa.projectoaa.core.network.model.register


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep

/**
{
"code": 500,
"data": null,
"message": "数据库插入失败"
}
 */
@Keep
@Serializable
data class RegisterErrorResponse(
    val code: Int,
    val data: String? = null,
    val message: String
)