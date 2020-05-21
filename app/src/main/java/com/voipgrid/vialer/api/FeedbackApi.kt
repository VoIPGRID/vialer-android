package com.voipgrid.vialer.api

import com.voipgrid.vialer.api.models.Feedback
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackApi {

    @POST("/v1/feedback/app")
    suspend fun submit(@Body feedback: Feedback): Response<Unit>
}