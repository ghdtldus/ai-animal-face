package com.example.android.data.model

data class ResultLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val animal: String,
    val score: Float,
    val date: String
)