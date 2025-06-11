package com.example.seglo.data.repository

import com.example.seglo.data.api.PredictionApi
import com.example.seglo.data.api.PredictionRequest
import com.example.seglo.data.api.PredictionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class PredictionRepository {
    private val api: PredictionApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/") // Use 10.0.2.2 for Android emulator to access localhost
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(PredictionApi::class.java)
    }

    suspend fun predictSign(imageBase64: String): Result<PredictionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = PredictionRequest(image = imageBase64)
            val response = api.predict(request)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 