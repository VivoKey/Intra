package com.carbidecowboy.intra.domain.request

data class ChallengeRequest(
    val scheme: Int,
    val message: String? = null,
    val uid: String? = null
)
