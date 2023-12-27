package com.hoker.intra.domain.request

data class SessionRequest(
    val uid: String,
    val response: String,
    val token: String,
    val cld: String? = null
)
