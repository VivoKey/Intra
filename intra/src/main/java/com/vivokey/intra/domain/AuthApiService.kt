package com.vivokey.intra.domain

import com.vivokey.intra.domain.request.AuthenticateRequest
import com.vivokey.intra.domain.request.ChallengeRequest
import com.vivokey.intra.domain.response.AuthenticateResponse
import com.vivokey.intra.domain.response.ChallengeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("challenge")
    suspend fun postChallenge(@Body challengeRequest: ChallengeRequest): Response<ChallengeResponse>

    /**
     * Authenticate with the VivoKey verify API using the encrypted JWE flow.
     * Returns an encrypted JWE token that must be decrypted by your server.
     * The mobile app passes the developer ID (dev_id) but never has the API key.
     */
    @POST("authenticate")
    suspend fun postAuthenticate(@Body authenticateRequest: AuthenticateRequest): Response<AuthenticateResponse>
}
