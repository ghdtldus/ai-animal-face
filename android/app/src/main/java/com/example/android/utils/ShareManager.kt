package com.example.android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareManager {

    fun shareToKakao(context: Context, text: String, imageUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (imageUri != null) "image/*" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            imageUri?.let { putExtra(Intent.EXTRA_STREAM, it) }
            setPackage("com.kakao.talk")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "카카오톡이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToInstagram(context: Context, imageUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            setPackage("com.instagram.android")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "인스타그램이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}