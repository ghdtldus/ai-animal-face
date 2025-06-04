package com.example.android.data.api

import com.example.android.data.model.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("gender") gender: RequestBody
    ): Response<UploadResponse>
}
