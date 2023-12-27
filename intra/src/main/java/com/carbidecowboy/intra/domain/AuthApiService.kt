package com.carbidecowboy.intra.domain

import com.carbidecowboy.intra.domain.request.ChallengeRequest
import com.carbidecowboy.intra.domain.request.SessionRequest
import com.carbidecowboy.intra.domain.response.ChallengeResponse
import com.carbidecowboy.intra.domain.response.SessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("challenge")
    suspend fun postChallenge(@Body challengeRequest: ChallengeRequest): Response<ChallengeResponse>

    @POST("session")
    suspend fun postSession(@Body sessionRequest: SessionRequest): Response<SessionResponse>
}