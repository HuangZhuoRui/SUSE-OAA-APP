package com.suseoaa.projectoaa.core.network.model.course

import kotlinx.serialization.Serializable

@Serializable
data class RSAKey(
    val modulus: String,
    val exponent: String
)
