package com.vivokey.intra.domain.response

/**
 * Response from the /authenticate endpoint.
 * Contains an encrypted JWE token that must be sent to your server for decryption.
 * The mobile app cannot decrypt this token - it's opaque to the client.
 */
data class AuthenticateResponse(
    val token: String  // Encrypted JWE
)
