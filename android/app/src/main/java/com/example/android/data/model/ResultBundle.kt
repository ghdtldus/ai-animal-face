package com.example.android.data.model

import com.example.android.data.model.AnimalScore

data class ResultBundle(
    val uploadResult: AnimalScore,
    val uploadMessage: String?,
    val topKResults: List<AnimalScore>,
    val shareCardUrl: String?,
    val uploadedImageUri: String?,
    val sharePageUrl: String?
)