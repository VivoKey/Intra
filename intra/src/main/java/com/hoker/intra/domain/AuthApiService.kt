package com.hoker.intra.domain

import com.hoker.intra.domain.request.ChallengeRequest
import com.hoker.intra.domain.request.SessionRequest
import com.hoker.intra.domain.response.ChallengeResponse
import com.hoker.intra.domain.response.SessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("challenge")
    suspend fun postChallenge(@Body challengeRequest: ChallengeRequest): Response<ChallengeResponse>

    @POST("session")
    suspend fun postSession(@Body sessionRequest: SessionRequest): Response<SessionResponse>
}