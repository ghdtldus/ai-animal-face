package com.example.android.data.model

data class AnimalScore(
    val animal: String,
    val score: Float
)

data class UploadResponse(
    val main_result: AnimalScore,
    val top_k: List<AnimalScore>,
    val message: String,
    val share_card_url: String? = null,
    val image_uri: String? = null   
)