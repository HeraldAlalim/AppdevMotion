package com.example.seglo.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class PredictionRequest(
    val image: String // Base64 encoded image
)

data class PredictionResponse(
    val success: Boolean,
    val prediction: String,
    val confidence: Double,
    val error: String? = null
)

interface PredictionApi {
    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): PredictionResponse
} 