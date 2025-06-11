package com.example.android.utils

import com.example.android.R

fun getAnimalImageRes(animal: String): Int {
    return when (animal) {
        "bear" -> R.drawable.ic_bear
        "cat" -> R.drawable.ic_cat
        "dog" -> R.drawable.ic_dog
        "deer" -> R.drawable.ic_deer
        "rabbit" -> R.drawable.ic_rabbit
        "wolf" -> R.drawable.ic_wolf
        "tiger" -> R.drawable.ic_tiger
        "snake" -> R.drawable.ic_snake
        "squirrel" -> R.drawable.ic_squirrel
        "turtle" -> R.drawable.ic_turtle
        "dinosaur" -> R.drawable.ic_dinosaur
        else -> R.drawable.ic_bear
    }
}
