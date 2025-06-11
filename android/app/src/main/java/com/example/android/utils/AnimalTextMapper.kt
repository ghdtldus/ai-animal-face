package com.example.android.utils

fun getKoreanAnimalName(animal: String): String {
    return when (animal) {
        "bear" -> "곰상"
        "cat" -> "고양이상"
        "dog" -> "강아지상"
        "deer" -> "사슴상"
        "rabbit" -> "토끼상"
        "wolf" -> "늑대상"
        "tiger" -> "호랑이상"
        "snake" -> "뱀상"
        "squirrel" -> "다람쥐상"
        "turtle" -> "거북이상"
        "dinosaur" -> "공룡상"
        else -> "동물상"
    }
}
