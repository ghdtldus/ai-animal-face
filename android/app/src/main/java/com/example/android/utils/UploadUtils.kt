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
import org.json.JSONObject

fun uploadImageToServer(
    imageFile: File,
    gender: String,
    onResult: (UploadResponse?) -> Unit,
    onError: (String) -> Unit
) {
    val requestFile = imageFile.asRequestBody("image/jpg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
    val genderPart = RequestBody.create("text/plain".toMediaTypeOrNull(), gender)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.api.uploadImage(body, genderPart)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    onResult(response.body())
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val message = try {
                        // 서버가 "detail" 또는 "message" 키로 에러를 보낸다면 모두 대응
                        val json = JSONObject(errorBodyStr ?: "")
                        when {
                            json.has("detail") -> json.getString("detail")
                            json.has("message") -> json.getString("message")
                            else -> "서버 오류가 발생했습니다."
                        }
                    } catch (e: Exception) {
                        "서버 오류가 발생했습니다."
                    }
                    Log.e("Upload", "Error: $message")
                    onError(message)
                }
            }
        } catch (e: Exception) {
            Log.e("Upload", "Exception: ${e.message}")
            withContext(Dispatchers.Main) {
                onError("서버 연결에 실패했습니다.")
            }
        }
    }
}
