package com.example.android.utils

import android.util.Log
import com.example.android.data.api.RetrofitClient
import com.example.android.data.model.UploadResponse
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

fun uploadImageToServer(
    imageFile: File,
    gender: String,
    onResult: (UploadResponse?) -> Unit
) {
    val requestFile = imageFile.asRequestBody("image/jpg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
    val genderPart = gender.toRequestBody("text/plain".toMediaTypeOrNull())

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.api.uploadImage(body, genderPart)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    Log.e("Upload", "Error: ${response.code()}")
                    onResult(null)
                }
            }
        } catch (e: Exception) {
            Log.e("Upload", "Exception: ${e.message}")
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }
}
