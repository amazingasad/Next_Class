package com.example.nextclass.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {

    /** Gemini REST v1beta generateContent. The key travels in a header so it never appears in a URL. */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: GenerateContentRequest,
    ): GenerateContentResponse

    /** Cheap authenticated call used by "Verify Key". */
    @GET("v1beta/models")
    suspend fun listModels(
        @Header("x-goog-api-key") apiKey: String,
    ): ModelListResponse
}
