package com.vivokey.intra.domain.request

data class AuthenticateRequest(
    val token: String,
    val response: String,
    val uid: String,
    val dev_id: String
)
